package com.wortart.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wortart.app.audio.PronunciationAction
import com.wortart.app.audio.AnswerSounds
import com.wortart.app.data.settings.AppSettings
import com.wortart.app.reminder.ReminderScheduler
import com.wortart.app.ui.components.LocalShowBengali
import com.wortart.app.ui.practice.CategoryPickerScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.wortart.app.audio.TextToSpeechHelper
import com.wortart.app.audio.actionFor
import com.wortart.app.audio.voiceStatus
import com.wortart.app.data.onboarding.WelcomeStore
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.ui.MainScreen
import com.wortart.app.ui.MainTab
import com.wortart.app.ui.admin.AdminRoute
import com.wortart.app.ui.admin.AdminViewModel
import com.wortart.app.ui.explore.ExploreRoute
import com.wortart.app.ui.explore.ExploreViewModel
import com.wortart.app.ui.flashcard.TrackPracticeTime
import com.wortart.app.ui.home.HomeScreen
import com.wortart.app.ui.home.firstNameOf
import com.wortart.app.ui.members.MembersRoute
import com.wortart.app.ui.members.MembersViewModel
import com.wortart.app.ui.practice.PracticeRoute
import com.wortart.app.ui.practice.PracticeViewModel
import com.wortart.app.ui.rules.GrammarRulesRoute
import com.wortart.app.ui.rules.RulesViewModel
import com.wortart.app.ui.stats.StatsRoute
import com.wortart.app.ui.stats.StatsViewModel
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtTheme
import com.wortart.app.ui.welcome.WelcomeScreen
import com.wortart.app.ui.worddetails.WordDetailsRoute
import com.wortart.app.ui.worddetails.WordDetailsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val ROUTE_WELCOME = 0
private const val ROUTE_MAIN = 1
private const val ROUTE_PRACTICE = 2
private const val ROUTE_DETAILS = 3
private const val ROUTE_CATEGORY = 4

/**
 * Hosts the app: the welcome screen on the first start, then the tabs (Home, Explore, Rules,
 * Stats, Profile), the full-screen flashcard session that opens from Home and the word details
 * page that opens from Explore.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as WortArtApp
        val repository = app.vocabRepository
        val statsRepository = app.statsRepository
        val welcomeStore = WelcomeStore(this)
        val settings = AppSettings(this)

        setContent {
            val showBengali by settings.showBengali.collectAsState()
            val readFlashcards by settings.readFlashcards.collectAsState()
            val remindersOn by settings.remindersOn.collectAsState()

            WortArtTheme {
              CompositionLocalProvider(LocalShowBengali provides showBengali) {
                val scope = rememberCoroutineScope()

                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                LaunchedEffect(remindersOn) {
                    if (remindersOn) {
                        ReminderScheduler.schedule(applicationContext)
                        if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        ReminderScheduler.cancel(applicationContext)
                    }
                }

                val speech = remember { TextToSpeechHelper(applicationContext) }
                DisposableEffect(speech) { onDispose { speech.shutdown() } }
                val speechState by speech.state.collectAsState()

                val answerSounds = remember { AnswerSounds(applicationContext) }
                DisposableEffect(answerSounds) { onDispose { answerSounds.release() } }

                val exploreViewModel: ExploreViewModel =
                    viewModel(factory = ExploreViewModel.factory(repository))
                val rulesViewModel: RulesViewModel =
                    viewModel(factory = RulesViewModel.factory(repository, app.lessonsRepository))
                val statsViewModel: StatsViewModel =
                    viewModel(factory = StatsViewModel.factory(statsRepository, app.activityLog))
                val practiceViewModel: PracticeViewModel =
                    viewModel(factory = PracticeViewModel.factory(repository, statsRepository))
                val membersViewModel: MembersViewModel = viewModel(
                    factory = MembersViewModel.factory(
                        membership = app.membershipRepository,
                        auth = app.authRepository,
                        users = app.userRepository,
                        invitations = app.invitationRepository,
                        cardsPlayed = { statsRepository.observeStats().first().cardsPlayed },
                        resetLocalData = { app.localDataResetter.reset() },
                    ),
                )
                val adminViewModel: AdminViewModel =
                    viewModel(factory = AdminViewModel.factory(app.adminRepository))

                var route by rememberSaveable {
                    mutableIntStateOf(if (welcomeStore.hasSeenWelcome) ROUTE_MAIN else ROUTE_WELCOME)
                }
                var tabIndex by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
                var showAdmin by rememberSaveable { mutableStateOf(false) }
                var detailsWordId by rememberSaveable { mutableStateOf<String?>(null) }

                val summary by statsViewModel.uiState.collectAsState()
                val favoritesCount by remember { repository.observeFavorites().map { it.size } }
                    .collectAsState(initial = 0)
                val session by app.membershipRepository.session.collectAsState()
                val isMember = MembershipPolicy.isActive(session.profile, System.currentTimeMillis())

                val speakWord: (String, String) -> Unit = { wordId, word ->
                    when (speechState.actionFor(wordId)) {
                        PronunciationAction.SPEAK -> speech.speak(word, wordId)
                        PronunciationAction.STOP -> speech.stop()
                        PronunciationAction.GERMAN_VOICE_MISSING -> toast(
                            "German voice is not installed. Add it in Settings › Text-to-speech output.",
                        )
                        PronunciationAction.UNAVAILABLE -> toast(
                            "Text-to-speech is not available on this device.",
                        )
                    }
                }

                // Back from another tab returns to Home before leaving the app.
                BackHandler(enabled = route == ROUTE_MAIN && tabIndex != MainTab.Home.ordinal) {
                    tabIndex = MainTab.Home.ordinal
                }

                Surface(modifier = Modifier.fillMaxSize(), color = WortArtColors.Background) {
                    when (route) {
                        ROUTE_WELCOME -> WelcomeScreen(
                            onGetStarted = {
                                welcomeStore.markSeen()
                                route = ROUTE_MAIN
                            },
                            onLogin = {
                                welcomeStore.markSeen()
                                tabIndex = MainTab.Profile.ordinal
                                route = ROUTE_MAIN
                            },
                            modifier = Modifier.safeDrawingPadding(),
                        )

                        ROUTE_PRACTICE -> {
                            TrackPracticeTime { seconds ->
                                scope.launch { statsRepository.addPracticeTime(seconds) }
                            }
                            PracticeRoute(
                                viewModel = practiceViewModel,
                                speakingCardId = speechState.speakingId,
                                onSpeak = speakWord,
                                onAnswerResult = answerSounds::play,
                                readAloud = readFlashcards,
                                reminders = remindersOn,
                                showBengali = showBengali,
                                onReadAloud = settings::setReadFlashcards,
                                onReminders = settings::setRemindersOn,
                                onShowBengali = settings::setShowBengali,
                                onOpenCategories = { route = ROUTE_CATEGORY },
                                onClose = {
                                    speech.stop()
                                    route = ROUTE_MAIN
                                },
                                modifier = Modifier.safeDrawingPadding(),
                            )
                        }

                        ROUTE_CATEGORY -> {
                            val counts by practiceViewModel.groupCounts.collectAsState()
                            val practiceState by practiceViewModel.uiState.collectAsState()
                            CategoryPickerScreen(
                                counts = counts,
                                selected = practiceState.config.group,
                                onLoad = practiceViewModel::loadGroupCounts,
                                onSelect = { group ->
                                    practiceViewModel.chooseGroup(group)
                                    route = ROUTE_PRACTICE
                                },
                                onBack = { route = ROUTE_PRACTICE },
                                modifier = Modifier.safeDrawingPadding(),
                            )
                        }

                        ROUTE_DETAILS -> {
                            val wordId = detailsWordId.orEmpty()
                            val detailsViewModel: WordDetailsViewModel = viewModel(
                                key = "details-$wordId",
                                factory = WordDetailsViewModel.factory(wordId, repository),
                            )
                            WordDetailsRoute(
                                viewModel = detailsViewModel,
                                isSpeaking = speechState.speakingId == wordId,
                                onSpeak = speakWord,
                                onBack = {
                                    speech.stop()
                                    route = ROUTE_MAIN
                                },
                                modifier = Modifier.safeDrawingPadding(),
                            )
                        }

                        else -> MainScreen(
                            selectedTab = MainTab.entries[tabIndex],
                            onSelectTab = { tabIndex = it.ordinal },
                            home = {
                                HomeScreen(
                                    userName = firstNameOf(session.user?.displayName),
                                    isMember = isMember,
                                    summary = summary,
                                    onOpenFlashcards = {
                                        practiceViewModel.startOrResume()
                                        route = ROUTE_PRACTICE
                                    },
                                    onOpenExplore = { tabIndex = MainTab.Explore.ordinal },
                                    onOpenRules = { tabIndex = MainTab.Rules.ordinal },
                                    onOpenStats = { tabIndex = MainTab.Stats.ordinal },
                                    onOpenSearch = { tabIndex = MainTab.Explore.ordinal },
                                    onOpenProfile = { tabIndex = MainTab.Profile.ordinal },
                                )
                            },
                            explore = {
                                ExploreRoute(
                                    viewModel = exploreViewModel,
                                    onOpenWord = { id ->
                                        detailsWordId = id
                                        route = ROUTE_DETAILS
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            rules = {
                                GrammarRulesRoute(viewModel = rulesViewModel, modifier = Modifier.fillMaxSize())
                            },
                            stats = {
                                StatsRoute(
                                    viewModel = statsViewModel,
                                    onStartPractice = {
                                        practiceViewModel.startOrResume()
                                        route = ROUTE_PRACTICE
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            profile = {
                                if (showAdmin) {
                                    AdminRoute(
                                        viewModel = adminViewModel,
                                        onBack = { showAdmin = false },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    MembersRoute(
                                        viewModel = membersViewModel,
                                        favoritesCount = favoritesCount,
                                        voiceStatus = speechState.voiceStatus(),
                                        onOpenAdmin = { showAdmin = true },
                                        onOpenFavorites = {
                                            exploreViewModel.showFavorites()
                                            tabIndex = MainTab.Explore.ordinal
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        )
                    }
                }
              }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
