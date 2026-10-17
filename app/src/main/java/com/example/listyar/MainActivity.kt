package com.example.listyar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val pipeCatalog = mapOf(
    "سفید" to listOf("20", "25", "32", "40", "50", "60"),
    "پلیکا" to listOf("40", "63", "110", "125"),
    "نیو پایپ" to listOf("16", "20", "25", "32")
)
private val fittingCatalog = mapOf(
    "سفید" to listOf("زانو ۴۵ درجه", "زانو ۹۰ درجه", "سه راهی", "بوشن", "تبدیل"),
    "پلیکا" to listOf("زانو ۹۰ درجه", "زانو ۴۵ درجه", "بوشن (سوکت)", "سه راهی"),
    "نیو پایپ" to listOf("زانو", "سه راه", "تبدیل", "رابط")
)
private data class ListItem(val pipeType: String, val fitting: String, val size: String, val quantity: String)
private data class SavedList(
    val id: String, val projectName: String, val address: String, val date: String,
    val listType: String, val items: List<ListItem>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ListyarApp()
                    }
                }
            }
        }
    }
}

@Composable
private fun ListyarApp() {
    val context = LocalContext.current
    val savedLists = remember { mutableStateListOf<SavedList>().apply { addAll(loadLists(context)) } }
    val draftItems = remember { mutableStateListOf<ListItem>() }
    var selectedTab by remember { mutableIntStateOf(1) }
    var screen by remember { mutableStateOf("home") }
    var projectName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var listDate by remember { mutableStateOf(today()) }
    var listType by remember { mutableStateOf("لیست خرید") }
    var pipeType by remember { mutableStateOf("سفید") }
    var fitting by remember { mutableStateOf(fittingCatalog.getValue("سفید").first()) }
    var size by remember { mutableStateOf(pipeCatalog.getValue("سفید").first()) }
    var quantity by remember { mutableStateOf("1") }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf("") }

    fun startNewList() {
        projectName = ""; address = ""; listDate = today(); listType = "لیست خرید"
        pipeType = "سفید"; fitting = fittingCatalog.getValue("سفید").first()
        size = pipeCatalog.getValue("سفید").first(); quantity = "1"
        draftItems.clear(); formError = ""; screen = "mainInfo"
    }
    fun persist() = saveLists(context, savedLists)

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selectedTab == 0, { selectedTab = 0; screen = "lists" },
                icon = { Icon(Icons.Outlined.ListAlt, contentDescription = "لیست‌ها") }, label = { Text("لیست‌ها") })
            NavigationBarItem(selectedTab == 1, { selectedTab = 1; screen = "home" },
                icon = { Icon(Icons.Outlined.Home, contentDescription = "خانه") }, label = { Text("خانه") })
            NavigationBarItem(selectedTab == 2, { selectedTab = 2; screen = "settings" },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات") }, label = { Text("تنظیمات") })
        }
    }) { padding ->
        when (screen) {
            "mainInfo" -> MainInfoPage(
                Modifier.padding(padding), projectName, { projectName = it }, address, { address = it },
                listDate, { listDate = it }, listType, { listType = it }, formError,
                { screen = "home" }, {
                    if (projectName.isBlank()) formError = "نام پروژه را وارد کن."
                    else { formError = ""; screen = "items" }
                }
            )
            "items" -> ItemsPage(
                Modifier.padding(padding), draftItems, pipeType, {
                    pipeType = it; fitting = fittingCatalog.getValue(it).first(); size = pipeCatalog.getValue(it).first()
                }, fitting, { fitting = it }, size, { size = it }, quantity, { quantity = it },
                formError, { screen = "mainInfo" }, {
                    val count = quantity.toIntOrNull()
                    if (count == null || count <= 0) formError = "تعداد باید عددی بزرگ‌تر از صفر باشد."
                    else { draftItems.add(ListItem(pipeType, fitting, size, count.toString())); quantity = "1"; formError = "" }
                }, { draftItems.remove(it) }, {
                    if (draftItems.isEmpty()) formError = "حداقل یک قلم به لیست اضافه کن."
                    else {
                        val record = SavedList(UUID.randomUUID().toString(), projectName.trim(), address.trim(),
                            listDate.trim().ifBlank { today() }, listType, draftItems.toList())
                        savedLists.add(0, record); persist(); selectedListId = record.id
                        formError = ""; screen = "detail"
                    }
                }
            )
            "lists" -> ListsPage(Modifier.padding(padding), savedLists,
                { selectedListId = it.id; screen = "detail" }, { savedLists.remove(it); persist() }, { startNewList() })
            "settings" -> SettingsPage(Modifier.padding(padding))
            "detail" -> {
                val record = savedLists.firstOrNull { it.id == selectedListId }
                if (record == null) SimplePage("جزئیات لیست", "این لیست پیدا نشد.", Modifier.padding(padding))
                else DetailPage(Modifier.padding(padding), record) { selectedTab = 0; screen = "lists" }
            }
            else -> HomePage(Modifier.padding(padding), savedLists.size, { startNewList() }) {
                selectedTab = 0; screen = "lists"
            }
        }
    }
}

@Composable
private fun HomePage(modifier: Modifier, savedCount: Int, onNewList: () -> Unit, onOpenLists: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("لیست‌یار", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("مدیریت دقیق‌تر لوله و اتصالات پروژه‌ها", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("ساخت لیست جدید", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("نام پروژه، آدرس و اقلام موردنیاز را ثبت کن.")
                Button(onNewList, Modifier.fillMaxWidth().height(52.dp)) { Text("شروع ثبت لیست") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لیست‌های ذخیره‌شده", style = MaterialTheme.typography.titleMedium)
                Text("$savedCount لیست", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onOpenLists, Modifier.fillMaxWidth()) { Text("مشاهده لیست‌ها") }
            }
        }
    }
}

@Composable
private fun MainInfoPage(
    modifier: Modifier, projectName: String, onProjectNameChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit, date: String, onDateChange: (String) -> Unit,
    listType: String, onListTypeChange: (String) -> Unit, error: String,
    onBack: () -> Unit, onContinue: () -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("اطلاعات اصلی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("مشخصات پروژه و نوع لیست را وارد کن.")
        OutlinedTextField(projectName, onProjectNameChange, Modifier.fillMaxWidth(), label = { Text("نام پروژه") }, singleLine = true)
        OutlinedTextField(address, onAddressChange, Modifier.fillMaxWidth(), label = { Text("آدرس پروژه") }, minLines = 2)
        OutlinedTextField(date, onDateChange, Modifier.fillMaxWidth(), label = { Text("تاریخ") }, singleLine = true)
        Text("نوع لیست", fontWeight = FontWeight.SemiBold)
        listOf("لیست خرید", "فاکتور", "لیست برای لوله‌فروش").forEach { option ->
            OutlinedButton({ onListTypeChange(option) }, Modifier.fillMaxWidth()) {
                Text(if (listType == option) "✓  $option" else option)
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onBack, Modifier.weight(1f)) { Text("بازگشت") }
            Button(onContinue, Modifier.weight(1f)) { Text("ادامه به اقلام") }
        }
    }
}

@Composable
private fun ItemsPage(
    modifier: Modifier, draftItems: List<ListItem>, pipeType: String, onPipeTypeChange: (String) -> Unit,
    fitting: String, onFittingChange: (String) -> Unit, size: String, onSizeChange: (String) -> Unit,
    quantity: String, onQuantityChange: (String) -> Unit, error: String, onBack: () -> Unit,
    onAddItem: () -> Unit, onRemoveItem: (ListItem) -> Unit, onSave: () -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("اقلام لیست", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("نوع لوله، اتصال، سایز و تعداد را انتخاب کن.")
        DropdownField("نوع لوله", pipeType, pipeCatalog.keys.toList(), onPipeTypeChange)
        DropdownField("نوع اتصال", fitting, fittingCatalog.getValue(pipeType), onFittingChange)
        DropdownField("سایز", size, pipeCatalog.getValue(pipeType), onSizeChange)
        OutlinedTextField(quantity, { onQuantityChange(it.filter(Char::isDigit)) }, Modifier.fillMaxWidth(),
            label = { Text("تعداد") }, singleLine = true)
        Button(onAddItem, Modifier.fillMaxWidth()) { Text("افزودن قلم به لیست") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (draftItems.isNotEmpty()) {
            Text("اقلام ثبت‌شده (${draftItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            draftItems.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${item.fitting} ${item.size} ${item.pipeType}", fontWeight = FontWeight.Medium)
                            Text("تعداد: ${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton({ onRemoveItem(item) }) { Text("حذف") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onBack, Modifier.weight(1f)) { Text("اطلاعات اصلی") }
            Button(onSave, Modifier.weight(1f)) { Text("ذخیره لیست") }
        }
    }
}

@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember(label) { mutableStateOf(false) }
    Column {
        OutlinedTextField(selected, {}, Modifier.fillMaxWidth().clickable { expanded = true },
            readOnly = true, label = { Text(label) }, singleLine = true)
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false }) }
        }
    }
}

@Composable
private fun ListsPage(
    modifier: Modifier, lists: List<SavedList>, onOpen: (SavedList) -> Unit,
    onDelete: (SavedList) -> Unit, onNew: () -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("لیست‌ها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onNew, Modifier.fillMaxWidth()) { Text("ساخت لیست جدید") }
        if (lists.isEmpty()) Text("هنوز لیستی ذخیره نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        lists.forEach { saved ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(saved.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${saved.date}  ·  ${saved.listType}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${saved.items.size} قلم", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ onOpen(saved) }, Modifier.weight(1f)) { Text("مشاهده") }
                        OutlinedButton({ onDelete(saved) }, Modifier.weight(1f)) { Text("حذف لیست") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPage(modifier: Modifier, savedList: SavedList, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("جزئیات لیست", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(savedList.projectName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (savedList.address.isNotBlank()) Text("آدرس: ${savedList.address}")
        Text("تاریخ: ${savedList.date}")
        Text("نوع لیست: ${savedList.listType}")
        Text("اقلام", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        savedList.items.forEachIndexed { index, item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${index + 1}. ${item.fitting} ${item.size} ${item.pipeType}", fontWeight = FontWeight.Medium)
                    Text("تعداد: ${item.quantity}")
                }
            }
        }
        Button(onBack, Modifier.fillMaxWidth()) { Text("بازگشت به لیست‌ها") }
    }
}

@Composable
private fun SettingsPage(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("لوله و اتصالات", fontWeight = FontWeight.SemiBold)
                Text("فهرست اولیه سفید، پلیکا و نیو پایپ در فرم ثبت اقلام فعال است.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ذخیره‌سازی", fontWeight = FontWeight.SemiBold)
                Text("لیست‌ها روی همین دستگاه ذخیره می‌شوند و به اینترنت نیاز ندارند.")
            }
        }
    }
}

@Composable
private fun SimplePage(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun today(): String = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())

private fun loadLists(context: Context): List<SavedList> = try {
    val raw = context.getSharedPreferences("listyar_data", Context.MODE_PRIVATE).getString("saved_lists", "[]") ?: "[]"
    val array = JSONArray(raw)
    buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val itemArray = obj.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (j in 0 until itemArray.length()) {
                    val item = itemArray.getJSONObject(j)
                    add(ListItem(item.optString("pipeType"), item.optString("fitting"), item.optString("size"), item.optString("quantity")))
                }
            }
            add(SavedList(obj.optString("id"), obj.optString("projectName"), obj.optString("address"),
                obj.optString("date"), obj.optString("listType"), items))
        }
    }
} catch (_: Exception) { emptyList() }

private fun saveLists(context: Context, lists: List<SavedList>) {
    val array = JSONArray()
    lists.forEach { saved ->
        val items = JSONArray()
        saved.items.forEach { item ->
            items.put(JSONObject().put("pipeType", item.pipeType).put("fitting", item.fitting)
                .put("size", item.size).put("quantity", item.quantity))
        }
        array.put(JSONObject().put("id", saved.id).put("projectName", saved.projectName)
            .put("address", saved.address).put("date", saved.date).put("listType", saved.listType).put("items", items))
    }
    context.getSharedPreferences("listyar_data", Context.MODE_PRIVATE).edit().putString("saved_lists", array.toString()).apply()
}
