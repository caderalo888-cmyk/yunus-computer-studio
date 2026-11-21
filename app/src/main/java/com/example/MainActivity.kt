package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studio.StudioRoutes
import com.example.studio.StudioViewModel
import com.example.ui.screens.AudioEditorScreen
import com.example.ui.screens.BackgroundRemovalScreen
import com.example.ui.screens.BanglaTtsScreen
import com.example.ui.screens.BillingReceiptScreen
import com.example.ui.screens.BusinessHubTabScreen
import com.example.ui.screens.CustomerManagementScreen
import com.example.ui.screens.CvMakerScreen
import com.example.ui.screens.DashboardTabScreen
import com.example.ui.screens.DualVoiceStudioScreen
import com.example.ui.screens.ElectionPosterScreen
import com.example.ui.screens.GraphicDesignStudioScreen
import com.example.ui.screens.IdPhotoMode
import com.example.ui.screens.IdPhotoStudioScreen
import com.example.ui.screens.IncomeExpenseScreen
import com.example.ui.screens.OfficeToolsScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.PhotoEnhanceScreen
import com.example.ui.screens.PrintStudioScreen
import com.example.ui.screens.QrGeneratorScreen
import com.example.ui.screens.SavedProjectsTabScreen
import com.example.ui.screens.StudioInfoTabScreen
import com.example.ui.screens.VisitingCardScreen
import com.example.ui.screens.WeddingCardScreen
import com.example.ui.theme.YunusStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudioViewModel = viewModel()
            val isDark by viewModel.darkTheme.collectAsStateWithLifecycle()

            YunusStudioTheme(darkTheme = isDark) {
                YunusStudioRoot(viewModel = viewModel, isDarkTheme = isDark)
            }
        }
    }
}

@Composable
private fun YunusStudioRoot(
    viewModel: StudioViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = StudioRoutes.DASHBOARD,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(StudioRoutes.DASHBOARD) {
            var activeTab by rememberSaveable { mutableIntStateOf(0) }
            val netBalance = transactions.filter { it.type == "INCOME" }.sumOf { it.amount } -
                transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val navItems = listOf(
                "Studio" to Icons.Rounded.Dashboard,
                "Projects" to Icons.Rounded.FolderSpecial,
                "Business" to Icons.Rounded.AccountBalanceWallet,
                "Info" to Icons.Rounded.Info
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 680.dp

                Scaffold(
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                                navItems.forEachIndexed { idx, (label, icon) ->
                                    NavigationBarItem(
                                        selected = activeTab == idx,
                                        onClick = { activeTab = idx },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (useRail) {
                            NavigationRail {
                                navItems.forEachIndexed { idx, (label, icon) ->
                                    NavigationRailItem(
                                        selected = activeTab == idx,
                                        onClick = { activeTab = idx },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                        when (activeTab) {
                            0 -> DashboardTabScreen(
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.toggleTheme() },
                                customers = customers,
                                invoices = invoices,
                                transactions = transactions,
                                projects = projects,
                                onOpenModule = { route -> navController.navigate(route) }
                            )
                            1 -> SavedProjectsTabScreen(
                                projects = projects,
                                onDeleteProject = { viewModel.deleteProject(it) },
                                onOpenModule = { route -> navController.navigate(route) }
                            )
                            2 -> BusinessHubTabScreen(
                                customersCount = customers.size,
                                invoicesCount = invoices.size,
                                netBalance = netBalance,
                                onOpenModule = { route -> navController.navigate(route) }
                            )
                            else -> StudioInfoTabScreen()
                        }
                    }
                }
            }
        }

        composable(StudioRoutes.PASSPORT) {
            IdPhotoStudioScreen(IdPhotoMode.PASSPORT, viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.VISA) {
            IdPhotoStudioScreen(IdPhotoMode.VISA, viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.NID) {
            IdPhotoStudioScreen(IdPhotoMode.NID, viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.PHOTO_ENHANCE) {
            PhotoEnhanceScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.BG_REMOVAL) {
            BackgroundRemovalScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.PDF_TOOLS) {
            PdfToolsScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.OFFICE_TOOLS) {
            OfficeToolsScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.GRAPHIC_DESIGN) {
            GraphicDesignStudioScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.WEDDING_CARD) {
            WeddingCardScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.ELECTION_POSTER) {
            ElectionPosterScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.VISITING_CARD) {
            VisitingCardScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.CV_MAKER) {
            CvMakerScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.BANGLA_TTS) {
            BanglaTtsScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.DUAL_VOICE) {
            DualVoiceStudioScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.AUDIO_EDITOR) {
            AudioEditorScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.PRINT_STUDIO) {
            PrintStudioScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.QR_GENERATOR) {
            QrGeneratorScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.CUSTOMERS) {
            CustomerManagementScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.BILLING) {
            BillingReceiptScreen(viewModel) { navController.popBackStack() }
        }
        composable(StudioRoutes.LEDGER) {
            IncomeExpenseScreen(viewModel) { navController.popBackStack() }
        }
    }
}
