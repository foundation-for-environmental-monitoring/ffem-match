@file:Suppress("FunctionName")

package io.ffem.lite.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ffem.lite.R
import io.ffem.lite.common.RESULT_ID
import io.ffem.lite.data.TestResult
import io.ffem.lite.model.MainViewModel
import io.ffem.lite.ui.theme.FfemliteTheme

class ResultListViewActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FfemliteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.results))
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                }) {
                                    Icon(Icons.Filled.ArrowBack, "backIcon", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorResource(
                                    R.color.colorPrimaryVariant
                                ),
                                titleContentColor = Color.White
                            ),
                        )

                        val owner = LocalViewModelStoreOwner.current

                        owner?.let {
                            val viewModel: MainViewModel = viewModel(
                                it,
                                "MainViewModel",
                                MainViewModelFactory(
                                    LocalContext.current.applicationContext
                                            as Application
                                )
                            )

                            ScreenSetup(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenSetup(viewModel: MainViewModel) {
    val testResults by viewModel.testResults.observeAsState(listOf())
    MainScreen(
        testResults = testResults
    )
}

@Composable
fun MainScreen(testResults: List<TestResult>) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
    ) {
        items(testResults) { testResult ->
            ResultRow(
                id = testResult.id, sampleType = testResult.sampleType,
                date = testResult.date, name = testResult.name,
                value = testResult.value, name2 = testResult.name2, value2 = testResult.value2,
                unit = testResult.unit
            )
            Divider(color = Color.Gray, thickness = 1.dp)
        }
    }
}

@Composable
fun ResultRow(
    id: String,
    sampleType: String,
    date: Long,
    name: String,
    value: Double,
    name2: String,
    value2: Double,
    unit: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
//                MainScope().launch {
//                    delay(300)
                val intent = Intent(context, ResultViewActivity::class.java)
                intent.putExtra(RESULT_ID, id)
                context.startActivity(intent)
//                }
            })
    ) {
        Column(
            modifier = Modifier
                .weight(0.68f)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(sampleType, fontSize = 13.sp, modifier = Modifier)
                Text(", ", fontSize = 13.sp, modifier = Modifier)
                Text(
                    DateUtils.getRelativeTimeSpanString(date).toString(), fontSize = 13.sp
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(name, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                Text(value.toString(), fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                Text(unit, fontSize = 14.sp)
            }
            if (value2 > -1) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(name2, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                    Text(
                        value2.toString(),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(unit, fontSize = 14.sp)
                }
            }
        }
    }
}

class MainViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(application) as T
    }
}