package com.example.listyar

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local relational SQLite storage for lists, items, pipe types, pipe sizes,
 * fittings and fitting sizes. Legacy SharedPreferences data is copied once.
 */
internal class ListyarDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "listyar.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE lists (id TEXT PRIMARY KEY NOT NULL, project TEXT NOT NULL, address TEXT NOT NULL, date TEXT NOT NULL, type TEXT NOT NULL)")
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, list_id TEXT NOT NULL, position INTEGER NOT NULL, pipe TEXT NOT NULL, fitting TEXT NOT NULL, size TEXT NOT NULL, quantity TEXT NOT NULL, FOREIGN KEY(list_id) REFERENCES lists(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_items_list_id ON items(list_id)")
        db.execSQL("CREATE TABLE pipes (name TEXT PRIMARY KEY NOT NULL, position INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE pipe_sizes (pipe_name TEXT NOT NULL, size TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(pipe_name, size), FOREIGN KEY(pipe_name) REFERENCES pipes(name) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE fittings (pipe_name TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(pipe_name, name), FOREIGN KEY(pipe_name) REFERENCES pipes(name) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE fitting_sizes (pipe_name TEXT NOT NULL, fitting_name TEXT NOT NULL, size TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(pipe_name, fitting_name, size), FOREIGN KEY(pipe_name, fitting_name) REFERENCES fittings(pipe_name, name) ON DELETE CASCADE)")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future versions must use explicit migrations; never drop user data here.
    }

    companion object {
        @Volatile private var instance: ListyarDatabase? = null
        fun get(context: Context): ListyarDatabase = instance ?: synchronized(this) {
            instance ?: ListyarDatabase(context).also { instance = it }
        }
    }
}

internal fun readLists(context: Context): List<SavedList> {
    val db = ListyarDatabase.get(context).writableDatabase
    migrateLegacyPreferences(context, db)
    val result = mutableListOf<SavedList>()
    db.rawQuery("SELECT id, project, address, date, type FROM lists ORDER BY rowid DESC", null).use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val items = mutableListOf<Item>()
            db.rawQuery("SELECT pipe, fitting, size, quantity FROM items WHERE list_id=? ORDER BY position, id", arrayOf(id)).use { itemCursor ->
                while (itemCursor.moveToNext()) items.add(Item(itemCursor.getString(0), itemCursor.getString(1), itemCursor.getString(2), itemCursor.getString(3)))
            }
            result.add(SavedList(id, cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), items))
        }
    }
    return result
}

internal fun writeLists(context: Context, lists: List<SavedList>) {
    val db = ListyarDatabase.get(context).writableDatabase
    migrateLegacyPreferences(context, db)
    db.beginTransaction()
    try {
        db.delete("items", null, null)
        db.delete("lists", null, null)
        lists.forEach { list ->
            db.execSQL("INSERT INTO lists(id, project, address, date, type) VALUES(?,?,?,?,?)", arrayOf(list.id, list.project, list.address, list.date, list.type))
            list.items.forEachIndexed { index, item ->
                db.execSQL("INSERT INTO items(list_id, position, pipe, fitting, size, quantity) VALUES(?,?,?,?,?,?)", arrayOf(list.id, index, item.pipe, item.fitting, item.size, item.quantity))
            }
        }
        db.setTransactionSuccessful()
    } finally { db.endTransaction() }
}

internal fun readPipes(context: Context): List<Pipe> {
    val db = ListyarDatabase.get(context).writableDatabase
    migrateLegacyPreferences(context, db)
    val result = mutableListOf<Pipe>()
    db.rawQuery("SELECT name FROM pipes ORDER BY position, rowid", null).use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            val sizes = mutableListOf<String>()
            db.rawQuery("SELECT size FROM pipe_sizes WHERE pipe_name=? ORDER BY position", arrayOf(name)).use { c -> while (c.moveToNext()) sizes.add(c.getString(0)) }
            val fittings = mutableListOf<Fitting>()
            db.rawQuery("SELECT name FROM fittings WHERE pipe_name=? ORDER BY position, rowid", arrayOf(name)).use { c ->
                while (c.moveToNext()) {
                    val fittingName = c.getString(0)
                    val fittingSizes = mutableListOf<String>()
                    db.rawQuery("SELECT size FROM fitting_sizes WHERE pipe_name=? AND fitting_name=? ORDER BY position", arrayOf(name, fittingName)).use { sc -> while (sc.moveToNext()) fittingSizes.add(sc.getString(0)) }
                    fittings.add(Fitting(fittingName, fittingSizes))
                }
            }
            result.add(Pipe(name, sizes, fittings))
        }
    }
    return result
}

internal fun writePipes(context: Context, pipes: List<Pipe>) {
    val db = ListyarDatabase.get(context).writableDatabase
    migrateLegacyPreferences(context, db)
    replacePipes(db, pipes)
}

private fun replacePipes(db: SQLiteDatabase, pipes: List<Pipe>) {
    db.beginTransaction()
    try {
        db.delete("fitting_sizes", null, null)
        db.delete("fittings", null, null)
        db.delete("pipe_sizes", null, null)
        db.delete("pipes", null, null)
        pipes.forEachIndexed { index, pipe ->
            db.execSQL("INSERT INTO pipes(name, position) VALUES(?,?)", arrayOf(pipe.name, index))
            pipe.sizes.forEachIndexed { sizeIndex, size -> db.execSQL("INSERT INTO pipe_sizes(pipe_name, size, position) VALUES(?,?,?)", arrayOf(pipe.name, size, sizeIndex)) }
            pipe.fittings.forEachIndexed { fittingIndex, fitting ->
                db.execSQL("INSERT INTO fittings(pipe_name, name, position) VALUES(?,?,?)", arrayOf(pipe.name, fitting.name, fittingIndex))
                fitting.sizes.forEachIndexed { sizeIndex, size -> db.execSQL("INSERT INTO fitting_sizes(pipe_name, fitting_name, size, position) VALUES(?,?,?,?)", arrayOf(pipe.name, fitting.name, size, sizeIndex)) }
            }
        }
        db.setTransactionSuccessful()
    } finally { db.endTransaction() }
}

private fun migrateLegacyPreferences(context: Context, db: SQLiteDatabase) {
    val prefs = context.getSharedPreferences("listyar_data", 0)
    if (prefs.getBoolean("sqlite_migration_complete", false)) return
    db.beginTransaction()
    try {
        val hasLists = db.rawQuery("SELECT COUNT(*) FROM lists", null).use { it.moveToFirst() && it.getInt(0) > 0 }
        val hasPipes = db.rawQuery("SELECT COUNT(*) FROM pipes", null).use { it.moveToFirst() && it.getInt(0) > 0 }
        if (!hasLists) {
            val raw = prefs.getString("saved_lists", "[]") ?: "[]"
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
                db.execSQL("INSERT OR REPLACE INTO lists(id, project, address, date, type) VALUES(?,?,?,?,?)", arrayOf(id, o.optString("projectName"), o.optString("address"), o.optString("date"), o.optString("listType")))
                val items = o.optJSONArray("items") ?: JSONArray()
                for (j in 0 until items.length()) {
                    val item = items.getJSONObject(j)
                    db.execSQL("INSERT INTO items(list_id, position, pipe, fitting, size, quantity) VALUES(?,?,?,?,?,?)", arrayOf(id, j, item.optString("pipeType"), item.optString("fitting"), item.optString("size"), item.optString("quantity")))
                }
            }
        }
        if (!hasPipes) {
            val raw = prefs.getString("catalog", null)
            val imported = if (raw.isNullOrBlank()) defaults() else {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val sizes = o.optJSONArray("sizes").toStringList()
                        val fittingsArray = o.optJSONArray("fittings") ?: JSONArray()
                        val fittings = buildList {
                            for (j in 0 until fittingsArray.length()) {
                                val f = fittingsArray.getJSONObject(j)
                                add(Fitting(f.optString("name"), f.optJSONArray("sizes").toStringList().ifEmpty { sizes }))
                            }
                        }
                        add(Pipe(o.optString("name"), sizes, fittings))
                    }
                }
            }
            replacePipesInsideTransaction(db, imported)
        }
        db.setTransactionSuccessful()
        prefs.edit().putBoolean("sqlite_migration_complete", true).apply()
    } catch (_: Exception) {
        // Leave the migration flag unset so the next open can retry safely.
    } finally { db.endTransaction() }
}

private fun replacePipesInsideTransaction(db: SQLiteDatabase, pipes: List<Pipe>) {
    db.delete("fitting_sizes", null, null); db.delete("fittings", null, null); db.delete("pipe_sizes", null, null); db.delete("pipes", null, null)
    pipes.forEachIndexed { index, pipe ->
        db.execSQL("INSERT INTO pipes(name, position) VALUES(?,?)", arrayOf(pipe.name, index))
        pipe.sizes.forEachIndexed { i, size -> db.execSQL("INSERT INTO pipe_sizes(pipe_name, size, position) VALUES(?,?,?)", arrayOf(pipe.name, size, i)) }
        pipe.fittings.forEachIndexed { i, fitting ->
            db.execSQL("INSERT INTO fittings(pipe_name, name, position) VALUES(?,?,?)", arrayOf(pipe.name, fitting.name, i))
            fitting.sizes.forEachIndexed { j, size -> db.execSQL("INSERT INTO fitting_sizes(pipe_name, fitting_name, size, position) VALUES(?,?,?,?)", arrayOf(pipe.name, fitting.name, size, j)) }
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    val arr = this ?: return emptyList()
    return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
}

internal data class ListyarBackup(val lists: List<SavedList>, val pipes: List<Pipe>)

internal fun exportBackup(context: Context): String {
    val lists = readLists(context)
    val pipes = readPipes(context)
    val root = JSONObject().put("app", "Listyar").put("backupVersion", 1).put("lists", JSONArray()).put("pipes", JSONArray())
    val listArray = root.getJSONArray("lists")
    lists.forEach { list ->
        val items = JSONArray()
        list.items.forEach { item -> items.put(JSONObject().put("pipeType", item.pipe).put("fitting", item.fitting).put("size", item.size).put("quantity", item.quantity)) }
        listArray.put(JSONObject().put("id", list.id).put("projectName", list.project).put("address", list.address).put("date", list.date).put("listType", list.type).put("items", items))
    }
    val pipeArray = root.getJSONArray("pipes")
    pipes.forEach { pipe ->
        val fittings = JSONArray()
        pipe.fittings.forEach { fitting -> fittings.put(JSONObject().put("name", fitting.name).put("sizes", JSONArray(fitting.sizes))) }
        pipeArray.put(JSONObject().put("name", pipe.name).put("sizes", JSONArray(pipe.sizes)).put("fittings", fittings))
    }
    return root.toString(2)
}

internal fun parseBackup(json: String): ListyarBackup {
    require(json.length <= 10_000_000) { "حجم فایل پشتیبان بیش از حد مجاز است." }
    val root = JSONObject(json)
    require(root.optString("app") == "Listyar" && root.optInt("backupVersion", -1) == 1) { "این فایل پشتیبان معتبر لیست‌یار نیست." }
    val listArray = root.optJSONArray("lists") ?: throw IllegalArgumentException("بخش لیست‌ها در فایل وجود ندارد.")
    val pipeArray = root.optJSONArray("pipes") ?: throw IllegalArgumentException("بخش کاتالوگ در فایل وجود ندارد.")
    val lists = mutableListOf<SavedList>()
    for (i in 0 until listArray.length()) {
        val o = listArray.getJSONObject(i)
        val itemsArray = o.optJSONArray("items") ?: JSONArray()
        val items = mutableListOf<Item>()
        for (j in 0 until itemsArray.length()) {
            val x = itemsArray.getJSONObject(j)
            items.add(Item(x.optString("pipeType"), x.optString("fitting"), x.optString("size"), x.optString("quantity")))
        }
        val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
        lists.add(SavedList(id, o.optString("projectName"), o.optString("address"), o.optString("date"), o.optString("listType"), items))
    }
    val pipes = mutableListOf<Pipe>()
    for (i in 0 until pipeArray.length()) {
        val o = pipeArray.getJSONObject(i)
        val sizes = o.optJSONArray("sizes").toStringList()
        val fittingsArray = o.optJSONArray("fittings") ?: JSONArray()
        val fittings = mutableListOf<Fitting>()
        for (j in 0 until fittingsArray.length()) {
            val f = fittingsArray.getJSONObject(j)
            fittings.add(Fitting(f.optString("name"), (f.optJSONArray("sizes").toStringList()).ifEmpty { sizes }))
        }
        pipes.add(Pipe(o.optString("name"), sizes, fittings))
    }
    require(lists.size <= 100_000 && pipes.size <= 10_000) { "تعداد داده‌های فایل غیرعادی است." }
    require(pipes.all { it.name.isNotBlank() && it.sizes.isNotEmpty() }) { "اطلاعات نوع لوله در فایل کامل نیست." }
    require(lists.all { it.project.isNotBlank() && it.items.all { item -> item.pipe.isNotBlank() && item.fitting.isNotBlank() && item.quantity.toIntOrNull()?.let { q -> q > 0 } == true } }) { "اطلاعات یکی از لیست‌ها در فایل کامل یا معتبر نیست." }
    return ListyarBackup(lists, pipes)
}

internal fun importBackup(context: Context, backup: ListyarBackup) {
    val db = ListyarDatabase.get(context).writableDatabase
    db.beginTransaction()
    try {
        db.delete("items", null, null); db.delete("lists", null, null)
        backup.lists.forEach { list ->
            db.execSQL("INSERT INTO lists(id, project, address, date, type) VALUES(?,?,?,?,?)", arrayOf(list.id, list.project, list.address, list.date, list.type))
            list.items.forEachIndexed { index, item -> db.execSQL("INSERT INTO items(list_id, position, pipe, fitting, size, quantity) VALUES(?,?,?,?,?,?)", arrayOf(list.id, index, item.pipe, item.fitting, item.size, item.quantity)) }
        }
        replacePipesInsideTransaction(db, backup.pipes)
        db.setTransactionSuccessful()
    } finally { db.endTransaction() }
    context.getSharedPreferences("listyar_data", 0).edit().putBoolean("sqlite_migration_complete", true).apply()
}
