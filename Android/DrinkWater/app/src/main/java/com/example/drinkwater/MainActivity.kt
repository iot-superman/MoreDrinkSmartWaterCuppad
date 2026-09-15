package com.example.drinkwater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.drinkwater.ui.theme.DrinkWaterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrinkWaterTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "page0" // 設 Page0 為初始頁
    ) {
        // Page0: 飲水主儀表板
        composable("page0") {
            Page0(
                onNavigateToSettings = { navController.navigate("page9") },
                onNavigateToManualIntake = { navController.navigate("page2") } // 點擊圓形 + 按鈕前往 Page2
            )
        }

        // Page2: 手動飲水/校準 (Step 1)
        composable("page2") {
            Page2(
                onNavigateToSettings = { navController.navigate("page9") }, // 點擊設定前往 Page9
                onBackClick = { navController.popBackStack() }
            )
        }

        // Page9: 設定頁面
        composable("page9") {
            Page9(
                onBackClick = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate("page0") },
                onNavigateToDeviceConnection = { navController.navigate("page3") }
            )
        }

        // Page3: 尋找藍牙設備
        composable("page3") {
            Page3(
                onNavigateToNext = { navController.navigate("page1") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Page1: Wi-Fi 密碼與配網設定
        composable("page1") {
            Page1(
                onNavigateToNext = { navController.navigate("page6") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Page6: 設備連線成功資訊頁
        // Page6: 設備連線成功資訊頁
        composable("page6") {
            Page6(
                onBackClick = { navController.navigate("page0") }
            )
        }
    }
}