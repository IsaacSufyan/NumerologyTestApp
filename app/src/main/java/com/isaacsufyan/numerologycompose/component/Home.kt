package com.isaacsufyan.numerologycompose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.isaacsufyan.numerologycompose.navigation.NavRoutes
import com.isaacsufyan.numerologycompose.numerology.NumerologyFinder
import com.isaacsufyan.numerologycompose.viewmodel.SharedViewModel
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun Home(navController: NavController, viewModel: SharedViewModel = SharedViewModel()) {
    val text: String = viewModel.inputValueLiveData.observeAsState().value ?: ""
    val selectedOption: Int = viewModel.pieValueStateFlow.collectAsState().value
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val radioOptions = listOf("Pie First Value", "Pie Last Value (static value)")
//    val (selectedOption, onOptionSelected) = remember { mutableStateOf(viewModel.state.collectAsState().value) }

    fun showSnackBar(msg: String) {
        coroutineScope.launch {
            snackBarHostState.currentSnackbarData?.dismiss()
            snackBarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ConstraintLayout(
            modifier = Modifier.padding(20.dp)
        ) {
            val (button, textField, snackBar, radioGroup) = createRefs()

            Column(
                modifier = Modifier.constrainAs(radioGroup) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
            ) {
                radioOptions.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (index == selectedOption),
                                onClick = {
                                    viewModel.pieValueStateFlow.value = index
                                    showSnackBar(text)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (index == selectedOption),
                            onClick = {
                                viewModel.pieValueStateFlow.value = index
                                showSnackBar(text)
                            }
                        )
                        Text(text = text)
                    }
                }
            }

            OutlinedTextField(
                value = text,
                label = {
                    Text(text = "Enter you lucky no between 0 to 1000")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                onValueChange = {
                    viewModel.inputValueLiveData.value = it
                },
                modifier = Modifier.constrainAs(textField) {
                    top.linkTo(radioGroup.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }, singleLine = true
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        val value = try {
                            text.toInt()
                        } catch (e: RuntimeException) {
                            null
                        }
                        if (value != null) {
                            if (value in 0..999) {
                                val result = NumerologyFinder.find(value, selectedOption)
                                viewModel.pieValueStateFlow.value = selectedOption
                                viewModel.inputValueLiveData.value = ""
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = "Result",
                                    value = result
                                )
                                navController.navigate(NavRoutes.NumerologyDetails.route)
                            } else showSnackBar("You Enter Wrong Input")
                        } else showSnackBar("You didn't input number")
                    } else showSnackBar("Please Enter Something")
                },
                modifier = Modifier.constrainAs(button) {
                    top.linkTo(textField.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) {
                Text("Press To Find Your Numerology")
            }

            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.constrainAs(snackBar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    linkTo(
                        button.bottom,
                        parent.bottom,
                        bias = 0.4F
                    )
                }
            )
        }
    }
}

@ExperimentalMaterial3Api
@Preview(showSystemUi = true)
@Composable
fun DefaultPreview() {
    Home(rememberNavController())
}