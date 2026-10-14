package com.example.listyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Rtl
                    ) {
                        ListyarApp()
                    }
                }
            }
        }
    }
}

@Composable
private fun ListyarApp() {
    var selectedTab by remember { mutableIntStateOf(1) }
    var showNewListNotice by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.ListAlt, contentDescription = "لیست‌ها") },
                    label = { Text("لیست‌ها") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "خانه") },
                    label = { Text("خانه") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات") },
                    label = { Text("تنظیمات") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> SimplePage(title = "لیست‌ها", description = "لیست‌های ذخیره‌شده در این بخش نمایش داده می‌شوند.", modifier = Modifier.padding(innerPadding))
            1 -> HomePage(
                modifier = Modifier.padding(innerPadding),
                onNewList = { showNewListNotice = 1 },
                showNotice = showNewListNotice == 1,
                onDismissNotice = { showNewListNotice = 0 }
            )
            else -> SimplePage(title = "تنظیمات", description = "مدیریت لوله و اتصالات، پوسته و پشتیبان‌گیری در این بخش قرار می‌گیرد.", modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun HomePage(
    modifier: Modifier = Modifier,
    onNewList: () -> Unit,
    showNotice: Boolean,
    onDismissNotice: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "لیست‌یار",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "مدیریت دقیق‌تر لوله و اتصالات پروژه‌ها",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("شروع یک لیست جدید", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "نام پروژه، آدرس و اقلام موردنیاز را ثبت کن تا لیستت مرتب بماند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onNewList,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("ساخت لیست جدید", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (showNotice) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مرحله بعد", fontWeight = FontWeight.SemiBold)
                    Text("صفحه‌های اطلاعات اصلی و اقلام لیست در گام بعدی پیاده‌سازی می‌شوند.")
                    Button(onClick = onDismissNotice, colors = ButtonDefaults.buttonColors()) { Text("متوجه شدم") }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "نسخه آغازین",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SimplePage(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
