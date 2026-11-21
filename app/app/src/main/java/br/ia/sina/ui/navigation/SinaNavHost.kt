package br.ia.sina.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.ia.sina.container
import br.ia.sina.data.AppState
import br.ia.sina.data.Role
import br.ia.sina.protection.ListenerStatus
import br.ia.sina.protection.ProtectionScheduler
import br.ia.sina.ui.elder.AlertScreen
import br.ia.sina.ui.elder.ConsentScreen
import br.ia.sina.ui.elder.ElderHomeScreen
import br.ia.sina.ui.elder.ElderSettingsScreen
import br.ia.sina.ui.elder.NotificationAccessScreen
import br.ia.sina.ui.elder.PairingCodeScreen
import br.ia.sina.ui.guardian.GuardianAlertDetailScreen
import br.ia.sina.ui.guardian.GuardianAlertsScreen
import br.ia.sina.ui.guardian.GuardianHomeScreen
import br.ia.sina.ui.guardian.GuardianInactiveScreen
import br.ia.sina.ui.guardian.GuardianPairScreen
import br.ia.sina.ui.guardian.GuardianSettingsScreen
import br.ia.sina.ui.guardian.GuardianTab
import br.ia.sina.ui.onboarding.OnboardingScreen
import br.ia.sina.ui.role.RoleScreen
import br.ia.sina.ui.signup.SignupScreen
import br.ia.sina.ui.splash.SplashScreen
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ROLE = "role"
    const val ELDER_CONSENT = "elder/consent"
    const val ELDER_SIGNUP = "elder/signup"
    const val ELDER_ACCESS = "elder/access"
    const val ELDER_HOME = "elder/home"
    const val ELDER_CODE = "elder/code"
    const val ELDER_SETTINGS = "elder/settings"
    const val ELDER_ALERT = "elder/alert/{id}"
    const val GUARDIAN_SIGNUP = "guardian/signup"
    const val GUARDIAN_PAIR = "guardian/pair"
    const val GUARDIAN_HOME = "guardian/home"
    const val GUARDIAN_ALERTS = "guardian/alerts"
    const val GUARDIAN_SETTINGS = "guardian/settings"
    const val GUARDIAN_ALERT = "guardian/alert/{id}"
    const val GUARDIAN_INACTIVE = "guardian/inactive/{id}"

    fun elderAlert(id: String) = "elder/alert/$id"
    fun guardianAlert(id: String) = "guardian/alert/$id"
    fun guardianInactive(protectedId: String) = "guardian/inactive/$protectedId"
}

/** Rotas que outros apps e as notificações podem abrir; o resto do app só é navegável por dentro. */
private val externalRoute = Regex("""(elder/alert|guardian/alert|guardian/inactive)/[A-Za-z0-9-]{1,64}""")

private fun NavHostController.replaceAll(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

@Composable
fun SinaNavHost(
    pendingRoute: String?,
    onRouteConsumed: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val prefs = context.container.prefs
    val scope = rememberCoroutineScope()
    val pending by rememberUpdatedState(pendingRoute)

    fun elderStart(state: AppState): String = when {
        !state.consentAccepted -> Routes.ELDER_CONSENT
        state.deviceToken == null && state.profile == null -> Routes.ELDER_SIGNUP
        !ListenerStatus.isEnabled(context) -> Routes.ELDER_ACCESS
        else -> Routes.ELDER_HOME
    }

    fun guardianStart(state: AppState): String = if (state.deviceToken == null) Routes.GUARDIAN_SIGNUP else Routes.GUARDIAN_HOME

    fun allowed(route: String, state: AppState): Boolean = when {
        !externalRoute.matches(route) -> false
        route.startsWith("elder/") -> state.role == Role.PROTECTED && state.consentAccepted
        else -> state.role == Role.GUARDIAN && state.deviceToken != null
    }

    fun openTab(tab: GuardianTab) {
        val route = when (tab) {
            GuardianTab.PEOPLE -> Routes.GUARDIAN_HOME
            GuardianTab.ALERTS -> Routes.GUARDIAN_ALERTS
            GuardianTab.SETTINGS -> Routes.GUARDIAN_SETTINGS
        }
        navController.navigate(route) {
            popUpTo(Routes.GUARDIAN_HOME)
            launchSingleTop = true
        }
    }

    LaunchedEffect(pendingRoute) {
        val target = pendingRoute ?: return@LaunchedEffect
        val current = navController.currentDestination?.route ?: return@LaunchedEffect
        if (current == Routes.SPLASH) return@LaunchedEffect
        if (allowed(target, prefs.current())) runCatching { navController.navigate(target) }
        onRouteConsumed()
    }

    NavHost(navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinish = {
                scope.launch {
                    val state = prefs.current()
                    val start = when {
                        state.role == Role.PROTECTED -> elderStart(state)
                        state.role == Role.GUARDIAN -> guardianStart(state)
                        state.onboardingDone -> Routes.ROLE
                        else -> Routes.ONBOARDING
                    }
                    navController.replaceAll(start)
                    val target = pending
                    if (target != null) {
                        if ((start == Routes.ELDER_HOME || start == Routes.GUARDIAN_HOME) && allowed(target, state)) runCatching { navController.navigate(target) }
                        onRouteConsumed()
                    }
                }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                scope.launch {
                    prefs.setOnboardingDone()
                    navController.replaceAll(Routes.ROLE)
                }
            })
        }
        composable(Routes.ROLE) {
            RoleScreen(onContinue = { role ->
                scope.launch {
                    prefs.setRole(role)
                    ProtectionScheduler.syncWithRole(context)
                    val state = prefs.current()
                    navController.navigate(if (role == Role.PROTECTED) elderStart(state) else guardianStart(state))
                }
            })
        }
        composable(Routes.ELDER_CONSENT) {
            ConsentScreen(
                onAccept = {
                    scope.launch {
                        prefs.setConsentAccepted()
                        ProtectionScheduler.start(context)
                        navController.navigate(elderStart(prefs.current())) {
                            popUpTo(Routes.ELDER_CONSENT) { inclusive = true }
                        }
                    }
                },
                onBack = { if (!navController.popBackStack()) navController.replaceAll(Routes.ROLE) },
            )
        }
        composable(Routes.ELDER_SIGNUP) {
            SignupScreen(
                role = Role.PROTECTED,
                onBack = { if (!navController.popBackStack()) navController.replaceAll(Routes.ROLE) },
                onDone = {
                    ProtectionScheduler.sendHeartbeatNow(context)
                    scope.launch { navController.replaceAll(elderStart(prefs.current())) }
                },
            )
        }
        composable(Routes.ELDER_ACCESS) {
            NotificationAccessScreen(
                onGranted = {
                    ProtectionScheduler.sendHeartbeatNow(context)
                    navController.replaceAll(Routes.ELDER_HOME)
                },
                onSkip = { navController.replaceAll(Routes.ELDER_HOME) },
            )
        }
        composable(Routes.ELDER_HOME) {
            ElderHomeScreen(
                onOpenAlert = { navController.navigate(Routes.elderAlert(it)) },
                onOpenSettings = { navController.navigate(Routes.ELDER_SETTINGS) },
                onLinkGuardian = { navController.navigate(Routes.ELDER_CODE) },
                onEnableAccess = { navController.navigate(Routes.ELDER_ACCESS) },
            )
        }
        composable(Routes.ELDER_CODE) {
            PairingCodeScreen(onBack = { navController.popBackStack() }, onPaired = { navController.popBackStack() })
        }
        composable(Routes.ELDER_SETTINGS) {
            ElderSettingsScreen(
                onBack = { navController.popBackStack() },
                onLinkGuardian = { navController.navigate(Routes.ELDER_CODE) },
                onAccountDeleted = { navController.replaceAll(Routes.ONBOARDING) },
            )
        }
        composable(Routes.ELDER_ALERT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            AlertScreen(
                alertId = entry.arguments?.getString("id").orEmpty(),
                onDone = { if (!navController.popBackStack(Routes.ELDER_HOME, inclusive = false)) navController.replaceAll(Routes.ELDER_HOME) },
                onAskHelp = { navController.navigate(Routes.ELDER_CODE) },
            )
        }
        composable(Routes.GUARDIAN_SIGNUP) {
            SignupScreen(
                role = Role.GUARDIAN,
                onBack = { if (!navController.popBackStack()) navController.replaceAll(Routes.ROLE) },
                onDone = {
                    navController.replaceAll(Routes.GUARDIAN_HOME)
                    navController.navigate(Routes.GUARDIAN_PAIR)
                },
            )
        }
        composable(Routes.GUARDIAN_PAIR) {
            GuardianPairScreen(onBack = { navController.popBackStack() }, onPaired = { navController.popBackStack(Routes.GUARDIAN_HOME, inclusive = false) })
        }
        composable(Routes.GUARDIAN_HOME) {
            GuardianHomeScreen(
                onTab = ::openTab,
                onAddPerson = { navController.navigate(Routes.GUARDIAN_PAIR) },
                onOpenAlert = { navController.navigate(Routes.guardianAlert(it)) },
                onOpenInactive = { navController.navigate(Routes.guardianInactive(it)) },
            )
        }
        composable(Routes.GUARDIAN_ALERTS) {
            GuardianAlertsScreen(onTab = ::openTab, onOpenAlert = { navController.navigate(Routes.guardianAlert(it)) })
        }
        composable(Routes.GUARDIAN_SETTINGS) {
            GuardianSettingsScreen(onTab = ::openTab, onAccountDeleted = { navController.replaceAll(Routes.ONBOARDING) })
        }
        composable(Routes.GUARDIAN_ALERT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            GuardianAlertDetailScreen(entry.arguments?.getString("id").orEmpty(), onBack = { navController.popBackStack() })
        }
        composable(Routes.GUARDIAN_INACTIVE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            GuardianInactiveScreen(entry.arguments?.getString("id").orEmpty(), onBack = { navController.popBackStack() })
        }
    }
}
