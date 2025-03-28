package com.github.yhanada.mockmylocation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.yhanada.mockmylocation.data.DataSource
import com.github.yhanada.mockmylocation.model.MyLocation
import com.github.yhanada.mockmylocation.ui.theme.MockMyLocationTheme

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
    var selectedIndex by remember { mutableStateOf(0) }
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
