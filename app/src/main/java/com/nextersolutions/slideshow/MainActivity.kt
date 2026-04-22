package com.nextersolutions.slideshow

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.nextersolutions.slideshow.ui.SlideshowScreen
import com.nextersolutions.slideshow.ui.theme.SlideshowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the screen awake while the slideshow is on display. This is a
        // blunt approach that suits a digital-signage style player.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            SlideshowTheme {
                SlideshowScreen()
            }
        }
    }
}
