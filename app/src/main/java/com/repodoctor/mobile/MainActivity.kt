
package com.repodoctor.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repodoctor.mobile.ui.RepoDoctorScreen
import com.repodoctor.mobile.viewmodel.RepoDoctorViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val repoDoctorViewModel: RepoDoctorViewModel = viewModel()

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF3159C9),
                    secondary = Color(0xFF5267A8)
                )
            ) {

                Surface {
                    RepoDoctorScreen(
                        vm = repoDoctorViewModel
                    )
                }
            }
        }
    }
}
