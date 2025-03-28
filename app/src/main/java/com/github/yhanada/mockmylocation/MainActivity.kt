package com.github.yhanada.mockmylocation

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.yhanada.mockmylocation.data.DataSource
import com.github.yhanada.mockmylocation.model.MyLocation
import com.github.yhanada.mockmylocation.ui.theme.MockMyLocationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MockMyLocationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MockMyLocation(
                        applicationContext = applicationContext,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MockMyLocation(applicationContext: Context, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        MockMyLocationScreenWithPermissionCheck(
            applicationContext = applicationContext,
            modifier = modifier
        )
    } else {
        MockMyLocationScreen(
            applicationContext = applicationContext,
            modifier = modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MockMyLocationScreenWithPermissionCheck(applicationContext: Context, modifier: Modifier = Modifier) {
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS
    )
    if (notificationPermissionState.status.isGranted) {
        MockMyLocationScreen(
            applicationContext = applicationContext,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier.padding(36.dp)
        ) {
            Text(
                text = "通知設定すると、位置偽装時に状態が通知エリアに表示されます。"
            )
            Spacer(modifier = modifier.padding(16.dp))
            Button(onClick = { notificationPermissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
        }
    }
}

@Composable
fun MockMyLocationScreen(applicationContext: Context, modifier: Modifier = Modifier) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val items = DataSource.getLocations()
    Column(
        modifier = modifier.padding(36.dp)
    ) {
        Button(
            onClick = {
                val s = items.getOrNull(selectedIndex) ?: return@Button
                MockMyLocationService.start(applicationContext, s)
            }
        ) {
            Text(text = "start")
        }

        Button(
            onClick = {
                MockMyLocationService.stop(applicationContext)
            }
        ) {
            Text(text = "stop")
        }

        Spacer(modifier = Modifier.padding(16.dp))
        DropdownList(items) { index ->
            selectedIndex = index
        }
    }
}

@Composable
fun DropdownList(
    items: List<MyLocation>,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
        Text(
            items[selectedIndex].name,
            modifier = Modifier.fillMaxWidth()
                .clickable(onClick = { expanded = true })
                .background(Color.Gray)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth().background(
                Color.Red)
        ) {
            items.forEachIndexed { index, s ->
                DropdownMenuItem(
                    text = {Text(text = s.name)},
                    onClick = {
                        selectedIndex = index
                        expanded = false
                        onSelected(index)
                    },
                )
            }
        }
    }
}
