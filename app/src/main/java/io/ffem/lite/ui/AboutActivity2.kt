package io.ffem.lite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.ui.ui.theme.FfemmatchTheme

@Suppress("FunctionName")
class AboutActivity2 : ComponentActivity() {
    private lateinit var openDialog: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultPreview()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        FfemmatchTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopAppBar(title = {
                    Text(text = stringResource(R.string.about))
                },
                    navigationIcon = {
                        IconButton(onClick = {
                            finish()
                        }) {
                            Icon(Icons.Filled.ArrowBack, "backIcon")
                        }
                    }
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 30.sp,
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    )

                    Text(
                        text = stringResource(R.string.app_slogan).uppercase(),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    )

                    Text(
                        text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                            }.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    )

                    val image: Painter =
                        painterResource(id = R.drawable.about_logo)
                    Image(
                        painter = image,
                        contentDescription = "",
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.defaultMinSize(80.dp)
                    )

                    Text(
                        text = stringResource(R.string.copyright),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable {
                            }.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    )

                    openDialog = remember { mutableStateOf(false) }
                    ShowNotices(openDialog)
                    Text(
                        text = stringResource(R.string.legal_info),
                        fontSize = 15.sp,
                        color = Color.Blue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                openDialog.value = true
                            }.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview(showBackground = true)
    @Composable
    fun Notices() {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(color = Color.White)
        ) {
            TopAppBar(title = {
                Text(text = stringResource(R.string.software_notices))
            },
                navigationIcon = {
                    IconButton(onClick = {
                        openDialog.value = false
                    }) {
                        Icon(Icons.Filled.ArrowBack, "backIcon")
                    }
                }
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Text(
                    text = stringResource(R.string.apache_license_2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.apache_software),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                        }.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.cca_license),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.cca_software),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                        }.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.eclipse_public_license),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.eclipse_software),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.mit_license),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )

                Text(
                    text = stringResource(R.string.mit_software),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                )
            }
        }
    }

    @Composable
    private fun ShowNotices(openDialog: MutableState<Boolean>) {
        if (openDialog.value) {
            Dialog(
                onDismissRequest = {
                    openDialog.value = false
                }) {
                Notices()
            }
        }
    }

}

