package com.example.listyar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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

private data class Fitting(val name: String, val sizes: List<String>)
private data class Pipe(val name: String, val sizes: List<String>, val fittings: List<Fitting>)
private data class Item(val pipe: String, val fitting: String, val size: String, val quantity: String)
private data class SavedList(val id: String, val project: String, val address: String, val date: String, val type: String, val items: List<Item>)

private fun defaults() = listOf(
    Pipe("سفید", listOf("20","25","32","40","50","60"), listOf("زانو ۴۵ درجه","زانو ۹۰ درجه","سه راهی","بوشن","تبدیل").map { Fitting(it,listOf("20","25","32","40","50","60")) }),
    Pipe("پلیکا", listOf("40","63","110","125"), listOf("زانو ۹۰ درجه","زانو ۴۵ درجه","بوشن (سوکت)","سه راهی").map { Fitting(it,listOf("40","63","110","125")) }),
    Pipe("نیو پایپ", listOf("16","20","25","32"), listOf("زانو","سه راه","تبدیل","رابط").map { Fitting(it,listOf("16","20","25","32")) })
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { ListyarApp() } }
    }
}

@Composable
private fun ListyarApp() {
    val context = LocalContext.current
    val lists = remember { mutableStateListOf<SavedList>().apply { addAll(readLists(context)) } }
    val pipes = remember { mutableStateListOf<Pipe>().apply { addAll(readPipes(context)) } }
    var theme by remember { mutableStateOf(context.getSharedPreferences("listyar_data",0).getString("theme","light") ?: "light") }
    val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        var page by remember { mutableStateOf("home") }
        var tab by remember { mutableIntStateOf(1) }
        var project by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(today()) }
        var listType by remember { mutableStateOf("لیست خرید") }
        val draft = remember { mutableStateListOf<Item>() }
        var selectedId by remember { mutableStateOf("") }
        var editId by remember { mutableStateOf("") }
        var search by remember { mutableStateOf("") }
        var pipeName by remember { mutableStateOf(pipes.firstOrNull()?.name ?: "") }
        var fittingName by remember { mutableStateOf("") }
        var size by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("1") }
        var editingIndex by remember { mutableIntStateOf(-1) }
        var message by remember { mutableStateOf("") }
        var managePipe by remember { mutableStateOf("") }
        var newPipeName by remember { mutableStateOf("") }
        var newSizes by remember { mutableStateOf("") }
        var newFitting by remember { mutableStateOf("") }

        fun saveAll() { writeLists(context, lists); writePipes(context, pipes) }
        fun choosePipe(name: String) {
            pipeName = name
            val p = pipes.firstOrNull { it.name == name }
            fittingName = p?.fittings?.firstOrNull()?.name ?: ""
            size = p?.sizes?.firstOrNull() ?: ""
        }
        fun newList() {
            editId = ""; project = ""; address = ""; date = today(); listType = "لیست خرید"
            draft.clear(); message = ""; editingIndex = -1
            choosePipe(pipes.firstOrNull()?.name ?: ""); quantity = "1"; page = "info"
        }
        fun editList(l: SavedList) {
            editId = l.id; project = l.project; address = l.address; date = l.date; listType = l.type
            draft.clear(); draft.addAll(l.items); message = ""; page = "info"
        }
        fun saveList() {
            if (project.isBlank()) { message = "نام پروژه را وارد کن."; page = "info"; return }
            if (draft.isEmpty()) { message = "حداقل یک قلم اضافه کن."; page = "items"; return }
            val id = if (editId.isBlank()) UUID.randomUUID().toString() else editId
            val record = SavedList(id, project.trim(), address.trim(), date.ifBlank { today() }, listType, draft.toList())
            val old = lists.indexOfFirst { it.id == id }
            if (old >= 0) lists[old] = record else lists.add(0, record)
            saveAll(); selectedId = id; message = ""; page = "detail"
        }
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab=0; page="lists" }, icon={ Icon(Icons.Outlined.ListAlt, contentDescription="لیست‌ها") }, label={ Text("لیست‌ها") })
                NavigationBarItem(tab == 1, { tab=1; page="home" }, icon={ Icon(Icons.Outlined.Home, contentDescription="خانه") }, label={ Text("خانه") })
                NavigationBarItem(tab == 2, { tab=2; page="settings" }, icon={ Icon(Icons.Outlined.Settings, contentDescription="تنظیمات") }, label={ Text("تنظیمات") })
            }
        }) { pad ->
            when (page) {
                "info" -> InfoPage(Modifier.padding(pad), project, {project=it}, address, {address=it}, date, {date=it}, listType, {listType=it}, message,
                    {page="home"}, { if(project.isBlank()) message="نام پروژه را وارد کن." else {message="";page="items"} })
                "items" -> {
                    val p = pipes.firstOrNull { it.name == pipeName }
                    ItemPage(Modifier.padding(pad), draft, pipes, pipeName, { choosePipe(it) }, fittingName, {fittingName=it}, size, {size=it}, quantity, {quantity=it}, message,
                        {page="info"},
                        {
                            val q=quantity.toIntOrNull()
                            if(q==null || q<1) message="تعداد باید بیشتر از صفر باشد."
                            else if(pipeName.isBlank() || fittingName.isBlank() || size.isBlank()) message="نوع لوله، اتصال و سایز را انتخاب کن."
                            else {
                                val it=Item(pipeName,fittingName,size,q.toString())
                                if(editingIndex>=0 && editingIndex<draft.size) draft[editingIndex]=it else draft.add(it)
                                editingIndex=-1;quantity="1";message=""
                            }
                        },
                        { draft.removeAt(it); editingIndex=-1 },
                        { index -> val it=draft[index]; editingIndex=index; choosePipe(it.pipe); fittingName=it.fitting; size=it.size; quantity=it.quantity },
                        { saveList() })
                }
                "lists" -> ListsPage(Modifier.padding(pad), lists.filter { search.isBlank() || it.project.contains(search,true) || it.address.contains(search,true) }, search, {search=it},
                    {selectedId=it.id;page="detail"}, {editList(it)}, {lists.remove(it);saveAll()}, {newList()})
                "detail" -> {
                    val l=lists.firstOrNull { it.id==selectedId }
                    if(l==null) Text("لیست پیدا نشد.", Modifier.padding(pad))
                    else DetailPage(Modifier.padding(pad),l,{tab=0;page="lists"},{editList(l)},{val cp=l.copy(id=UUID.randomUUID().toString(),date=today());lists.add(0,cp);saveAll();selectedId=cp.id;page="detail"})
                }
                "settings" -> SettingsPage(Modifier.padding(pad),theme,{theme=it;context.getSharedPreferences("listyar_data",0).edit().putString("theme",it).apply()},
                    pipes,managePipe,{managePipe=it},newPipeName,{newPipeName=it},newSizes,{newSizes=it},newFitting,{newFitting=it},
                    {
                        val n=newPipeName.trim();val s=parseSizes(newSizes)
                        if(n.isBlank()||s.isEmpty()) message="نام و حداقل یک سایز وارد کن."
                        else if(pipes.any{it.name.equals(n,true)}) message="این نوع لوله از قبل وجود دارد."
                        else {pipes.add(Pipe(n,s, emptyList()));writePipes(context,pipes);newPipeName="";newSizes="";message=""}
                    },
                    {old,n,s -> val i=pipes.indexOfFirst{it.name==old};if(i>=0&&n.isNotBlank()&&s.isNotEmpty()){pipes[i]=pipes[i].copy(name=n.trim(),sizes=s,fittings=pipes[i].fittings.map{f->f.copy(sizes=s)});if(pipeName==old)choosePipe(n.trim());writePipes(context,pipes);managePipe=n.trim()}},
                    {name,f -> val i=pipes.indexOfFirst{it.name==name};if(i>=0&&f.isNotBlank()&&pipes[i].fittings.none{it.name==f.trim()}){pipes[i]=pipes[i].copy(fittings=pipes[i].fittings+Fitting(f.trim(),pipes[i].sizes));writePipes(context,pipes);newFitting=""}},
                    {name,f -> val i=pipes.indexOfFirst{it.name==name};if(i>=0){pipes[i]=pipes[i].copy(fittings=pipes[i].fittings.filterNot{it.name==f});writePipes(context,pipes)}},
                    {name -> pipes.removeAll{it.name==name};writePipes(context,pipes);if(pipeName==name)choosePipe(pipes.firstOrNull()?.name?:"");managePipe=""},message,{message=it})
                else -> HomePage(Modifier.padding(pad),lists.size,{newList()},{tab=0;page="lists"})
            }
        }
    }
}

@Composable private fun HomePage(m:Modifier,count:Int,onNew:()->Unit,onLists:()->Unit) {
    Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("لیست‌یار",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("مدیریت دقیق‌تر لوله و اتصالات پروژه‌ها",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("ساخت لیست جدید",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
            Text("اطلاعات پروژه و اقلام موردنیاز را ثبت کن.")
            Button(onNew,Modifier.fillMaxWidth().height(52.dp)){Text("شروع ثبت لیست")}
        }}
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("لیست‌های ذخیره‌شده");Text(count.toString()+" لیست",color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onLists,Modifier.fillMaxWidth()){Text("مشاهده لیست‌ها")}
        }}
    }
}

@Composable private fun InfoPage(m:Modifier,project:String,onProject:(String)->Unit,address:String,onAddress:(String)->Unit,date:String,onDate:(String)->Unit,type:String,onType:(String)->Unit,error:String,onBack:()->Unit,onNext:()->Unit) {
    Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("اطلاعات اصلی",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(project,onProject,Modifier.fillMaxWidth(),label={Text("نام پروژه")},singleLine=true)
        OutlinedTextField(address,onAddress,Modifier.fillMaxWidth(),label={Text("آدرس پروژه")},minLines=2)
        OutlinedTextField(date,onDate,Modifier.fillMaxWidth(),label={Text("تاریخ")},singleLine=true)
        Text("نوع لیست",fontWeight=FontWeight.SemiBold)
        listOf("لیست خرید","لیست برای لوله‌فروش","فاکتور").forEach{v->OutlinedButton({onType(v)},Modifier.fillMaxWidth()){Text((if(type==v)"✓  " else "")+v)}}
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onBack,Modifier.weight(1f)){Text("بازگشت")};Button(onNext,Modifier.weight(1f)){Text("ادامه به اقلام")}}
    }
}

@Composable private fun ItemPage(m:Modifier,draft:List<Item>,pipes:List<Pipe>,pipe:String,onPipe:(String)->Unit,fitting:String,onFitting:(String)->Unit,size:String,onSize:(String)->Unit,qty:String,onQty:(String)->Unit,error:String,onBack:()->Unit,onAdd:()->Unit,onRemove:(Int)->Unit,onEdit:(Int)->Unit,onSave:()->Unit) {
    val p=pipes.firstOrNull{it.name==pipe}
    Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("اقلام لیست",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Drop("نوع لوله",pipe,pipes.map{it.name},onPipe)
        Drop("نوع اتصال",fitting,p?.fittings?.map{it.name}?:emptyList(),onFitting)
        Drop("سایز",size,p?.sizes?:emptyList(),onSize)
        OutlinedTextField(qty,onQty,Modifier.fillMaxWidth(),label={Text("تعداد")},singleLine=true)
        Button(onAdd,Modifier.fillMaxWidth()){Text(if(draft.isEmpty())"افزودن قلم به لیست" else "افزودن / ذخیره قلم")}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        draft.forEachIndexed{i,it->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text(it.fitting+" "+it.size+" "+it.pipe,fontWeight=FontWeight.Medium);Text("تعداد: "+it.quantity)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({onEdit(i)}){Text("ویرایش")};OutlinedButton({onRemove(i)}){Text("حذف")}}
        }}}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onBack,Modifier.weight(1f)){Text("اطلاعات اصلی")};Button(onSave,Modifier.weight(1f)){Text("ذخیره لیست")}}
    }
}

@Composable private fun Drop(label:String,selected:String,options:List<String>,onSelect:(String)->Unit){
    var expanded by remember(label){mutableStateOf(false)}
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        // Place a transparent click target above the read-only text field.
        // This avoids touch events being consumed by TextField on some devices.
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = options.isNotEmpty()) { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable private fun ListsPage(m:Modifier,lists:List<SavedList>,search:String,onSearch:(String)->Unit,onOpen:(SavedList)->Unit,onEdit:(SavedList)->Unit,onDelete:(SavedList)->Unit,onNew:()->Unit){
    Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("سابقه پروژه‌ها",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(search,onSearch,Modifier.fillMaxWidth(),label={Text("جست‌وجوی نام یا آدرس پروژه")},singleLine=true)
        Button(onNew,Modifier.fillMaxWidth()){Text("ساخت لیست جدید")}
        if(lists.isEmpty())Text("لیستی مطابق جست‌وجو پیدا نشد.")
        lists.forEach{l->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Text(l.project,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleMedium)
            Text(l.date+" · "+l.type,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(l.address.isNotBlank())Text(l.address,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(l.items.size.toString()+" قلم")
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton({onOpen(l)}){Text("مشاهده")};OutlinedButton({onEdit(l)}){Text("ویرایش")};OutlinedButton({onDelete(l)}){Text("حذف")}}
        }}}
    }
}

@Composable private fun DetailPage(m:Modifier,l:SavedList,onBack:()->Unit,onEdit:()->Unit,onCopy:()->Unit){
    Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("جزئیات لیست",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(l.project,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        if(l.address.isNotBlank())Text("آدرس: "+l.address)
        Text("تاریخ: "+l.date);Text("نوع لیست: "+l.type);Text("اقلام",style=MaterialTheme.typography.titleMedium)
        l.items.forEachIndexed{i,it->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text((i+1).toString()+". "+it.fitting+" "+it.size+" "+it.pipe);Text("تعداد: "+it.quantity)}}}
        Button(onEdit,Modifier.fillMaxWidth()){Text("ویرایش لیست")}
        OutlinedButton(onCopy,Modifier.fillMaxWidth()){Text("ساخت کپی از لیست")}
        OutlinedButton(onBack,Modifier.fillMaxWidth()){Text("بازگشت")}
    }
}

@Composable private fun SettingsPage(m:Modifier,theme:String,onTheme:(String)->Unit,pipes:List<Pipe>,selected:String?,onSelect:(String)->Unit,newName:String,onNewName:(String)->Unit,newSizes:String,onNewSizes:(String)->Unit,newFitting:String,onNewFitting:(String)->Unit,onAddPipe:()->Unit,onEditPipe:(String,String,List<String>)->Unit,onAddFitting:(String,String)->Unit,onDeleteFitting:(String,String)->Unit,onDeletePipe:(String)->Unit,error:String,onError:(String)->Unit){
    var editName by remember(selected){mutableStateOf(selected.orEmpty())}
    var editSizes by remember(selected,pipes){mutableStateOf(pipes.firstOrNull{it.name==selected}?.sizes?.joinToString(", ").orEmpty())}
    Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("تنظیمات",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text("تم برنامه",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            listOf("روشن" to "light","تاریک" to "dark","مطابق سیستم" to "system").forEach{(label,v)->Row(Modifier.fillMaxWidth().clickable{onTheme(v)},horizontalArrangement=Arrangement.SpaceBetween){Text(label);RadioButton(theme==v,{onTheme(v)})}}
        }}
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("لوله‌ها و اتصالات",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            pipes.forEach{p->
                OutlinedButton({onSelect(if(selected==p.name) "" else p.name)},Modifier.fillMaxWidth()){Text(p.name+" · "+p.sizes.size+" سایز · "+p.fittings.size+" اتصال")}
                if(selected==p.name){
                    OutlinedTextField(editName,{editName=it},Modifier.fillMaxWidth(),label={Text("نام نوع لوله")},singleLine=true)
                    OutlinedTextField(editSizes,{editSizes=it},Modifier.fillMaxWidth(),label={Text("سایزها با ویرگول")})
                    Button({onEditPipe(p.name,editName,parseSizes(editSizes))},Modifier.fillMaxWidth()){Text("ذخیره نام و سایزها")}
                    OutlinedTextField(newFitting,onNewFitting,Modifier.fillMaxWidth(),label={Text("نام اتصال جدید")},singleLine=true)
                    Button({onAddFitting(p.name,newFitting)},Modifier.fillMaxWidth()){Text("افزودن اتصال")}
                    p.fittings.forEach{f->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(f.name);Text("سایزها: "+f.sizes.joinToString("، "),style=MaterialTheme.typography.bodySmall)};TextButton({onDeleteFitting(p.name,f.name)}){Text("حذف")}}}
                    TextButton({onDeletePipe(p.name)}){Text("حذف نوع لوله")}
                }
            }
            Divider()
            Text("افزودن نوع لوله",fontWeight=FontWeight.SemiBold)
            OutlinedTextField(newName,onNewName,Modifier.fillMaxWidth(),label={Text("نام نوع لوله")},singleLine=true)
            OutlinedTextField(newSizes,onNewSizes,Modifier.fillMaxWidth(),label={Text("سایزها، جداشده با ویرگول")})
            Button(onAddPipe,Modifier.fillMaxWidth()){Text("افزودن نوع لوله")}
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        }}
    }
}

private fun parseSizes(s:String)=s.split(',', '،',';','؛',' ','\n').map{it.trim()}.filter{it.isNotBlank()}.distinct()
private fun today()=SimpleDateFormat("yyyy/MM/dd",Locale.US).format(Date())

private fun readPipes(c:Context):List<Pipe> = try{
    val raw=c.getSharedPreferences("listyar_data",0).getString("catalog",null)
    if(raw.isNullOrBlank())defaults() else {val a=JSONArray(raw);buildList{for(i in 0 until a.length()){val o=a.getJSONObject(i);val sz=o.optJSONArray("sizes").strings();val fs=o.optJSONArray("fittings")?:JSONArray();val f=buildList{for(j in 0 until fs.length()){val x=fs.getJSONObject(j);add(Fitting(x.optString("name"),x.optJSONArray("sizes").strings().ifEmpty{sz}))}};add(Pipe(o.optString("name"),sz,f))}}}
}catch(_:Exception){defaults()}
private fun writePipes(c:Context,pipes:List<Pipe>){val a=JSONArray();pipes.forEach{p->val f=JSONArray();p.fittings.forEach{x->f.put(JSONObject().put("name",x.name).put("sizes",JSONArray(x.sizes)))};a.put(JSONObject().put("name",p.name).put("sizes",JSONArray(p.sizes)).put("fittings",f))};c.getSharedPreferences("listyar_data",0).edit().putString("catalog",a.toString()).apply()}
private fun JSONArray?.strings():List<String>{val a=this?:return emptyList();return buildList{for(i in 0 until a.length())add(a.optString(i))}}
private fun readLists(c:Context):List<SavedList> = try{val a=JSONArray(c.getSharedPreferences("listyar_data",0).getString("saved_lists","[]")?: "[]");buildList{for(i in 0 until a.length()){val o=a.getJSONObject(i);val ia=o.optJSONArray("items")?:JSONArray();val items=buildList{for(j in 0 until ia.length()){val x=ia.getJSONObject(j);add(Item(x.optString("pipeType"),x.optString("fitting"),x.optString("size"),x.optString("quantity")))}};add(SavedList(o.optString("id",UUID.randomUUID().toString()),o.optString("projectName"),o.optString("address"),o.optString("date"),o.optString("listType"),items))}}}catch(_:Exception){emptyList()}
private fun writeLists(c:Context,lists:List<SavedList>){val a=JSONArray();lists.forEach{l->val items=JSONArray();l.items.forEach{x->items.put(JSONObject().put("pipeType",x.pipe).put("fitting",x.fitting).put("size",x.size).put("quantity",x.quantity))};a.put(JSONObject().put("id",l.id).put("projectName",l.project).put("address",l.address).put("date",l.date).put("listType",l.type).put("items",items))};c.getSharedPreferences("listyar_data",0).edit().putString("saved_lists",a.toString()).apply()}
