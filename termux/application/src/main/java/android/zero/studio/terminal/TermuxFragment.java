package android.zero.studio.terminal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import android.zero.studio.terminal.app.BaseIDEFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.itsaky.androidide.projects.IProjectManager;
import com.termux.R;
import com.termux.app.activities.HelpActivity;
import android.zero.studio.terminal.api.file.FileReceiverActivity;
import android.zero.studio.terminal.terminal.TermuxTerminalSessionFragmentClient;
import android.zero.studio.terminal.terminal.TermuxTerminalViewClient;
import android.zero.studio.terminal.terminal.TermuxSessionsListViewController;
import android.zero.studio.terminal.io.TerminalToolbarViewPager;
import android.zero.studio.terminal.io.TermuxTerminalExtraKeys;
import android.zero.studio.terminal.ui.tabs.SessionTabManager;
import android.zero.studio.terminal.ui.tabs.TerminalSessionTabsView;

import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.ViewUtils;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A terminal emulator fragment. This class encapsulates the UI and logic for a terminal interface,
 * designed to be hosted by any Activity.
 *
 * @author android_zero
 */
public class TermuxFragment extends BaseIDEFragment implements ServiceConnection {

    /** The root view of the fragment, inflated in onCreateView. */
    private View mRootView;

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a
     * call to {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    protected TermuxService mTermuxService;

    /** The {@link TerminalView} shown in {@link TermuxFragment} that displays the terminal. */
    protected TerminalView mTerminalView;

    /**
     * The {@link TerminalViewClient} interface implementation to allow for communication between
     * {@link TerminalView} and {@link TermuxFragment}.
     */
    protected TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     * The {@link TerminalSessionClient} interface implementation to allow for communication between
     * {@link TerminalSession} and {@link TermuxFragment}.
     */
    protected TermuxTerminalSessionFragmentClient mTermuxTerminalSessionActivityClient;

    /** Termux app shared preferences manager. */
    protected TermuxAppSharedPreferences mPreferences;

    /** Termux app SharedProperties loaded from termux.properties */
    protected TermuxAppSharedProperties mProperties;

    /** The root view of the {@link TermuxFragment}. */
    protected TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxFragment}.
     * Used for legacy layout calculations or padding adjustments.
     */
    protected View mTermuxActivityBottomSpaceView;

    /** The terminal extra keys view. */
    protected ExtraKeysView mExtraKeysView;

    /** The client for the {@link #mExtraKeysView}. */
    protected TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /** The termux sessions list controller (kept for compatibility, though UI uses Tabs now). */
    protected TermuxSessionsListViewController mTermuxSessionListViewController;

    /**
     * The last toast shown, used cancel current toast before showing new in {@link
     * #showToast(String, boolean)}.
     */
    protected Toast mLastToast;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the
     * terminal view at the time, so if the session causing a change is not in the foreground it
     * should probably be treated as background.
     */
    protected boolean mIsVisible;

    /** If onResume() was called after onCreate(). */
    protected boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If activity was restarted like due to call to {@link #recreate()} after receiving {@link
     * TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity was
     * killed by android.
     */
    protected boolean mIsActivityRecreated = false;

    /** The {@link TermuxFragment} is in an invalid state and must not be run. */
    protected boolean mIsInvalidState;

    protected int mNavBarHeight;

    protected float mTerminalToolbarDefaultHeight;

    protected TerminalSessionTabsView mTabsView;
    protected SessionTabManager mSessionTabManager;

    // Bottom Sheet specific variables
    private View mBottomSheetContainer;
    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private View mDragHandleView;

    // Project specific
    private String mInitialWorkingDir;
    private String mInitialSessionName;

    protected static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    protected static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    protected static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    protected static final int CONTEXT_MENU_AUTOFILL_ID = 2;
    protected static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    protected static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    protected static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;
    protected static final int CONTEXT_MENU_HELP_ID = 7;
    protected static final int CONTEXT_MENU_REPORT_ID = 9;

    protected static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    protected static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    // Arguments key
    public static final String ARG_WORKING_DIR = "arg_working_dir";

    protected static final String LOG_TAG = "TermuxFragment";

    // This method is from the original BaseIDEActivity and now adapted for BaseIDEFragment
    @NonNull
    @Override
    protected View bindLayout(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return inflater.inflate(R.layout.fragment_terminal, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        TermuxAppSharedProperties.init(context);
        mProperties = TermuxAppSharedProperties.getProperties();
        setActivityTheme();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Logger.logDebug(LOG_TAG, "onCreate");
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        // Retrieve working directory from arguments or IProjectManager
        if (getArguments() != null) {
            mInitialWorkingDir = getArguments().getString(ARG_WORKING_DIR);
        }

        // Fallback to Project Manager if null
        // Also added checks to prevent crashes if IProjectManager is not available.
        if (mInitialWorkingDir == null) {
            try {

                IProjectManager projectManager = IProjectManager.getInstance();
                if (projectManager != null) {
                    // Assuming projectDirPath property exists or getProjectDir()
                    String path = projectManager.getProjectDirPath();
                    File projectDir = new File(path);
                    if (projectDir.exists() && projectDir.isDirectory()) {
                        mInitialWorkingDir = projectDir.getAbsolutePath();
                        // Also try to get project name
                        if (IProjectManager.getInstance().getWorkspace() != null &&
                                IProjectManager.getInstance().getWorkspace().getRootProject() != null) {
                            mInitialSessionName = IProjectManager.getInstance().getWorkspace().getRootProject().getName();
                        }
                    }
                }
            } catch (NoClassDefFoundError | Exception e) {
                // IProjectManager might not be available in all contexts or builds
                Logger.logWarn(LOG_TAG, "IProjectManager not available or failed to get project info: " + e.getMessage());
            }
        }

        ReportActivity.deleteReportInfoFilesOlderThanXDays(requireActivity(), 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(requireActivity(), true);
        if (mPreferences == null) {
            // An AlertDialog should have shown to kill the app, so we don't continue running
            // fragment code
            mIsInvalidState = true;
            return;
        }

        // The service binding is initiated here and callbacks are handled by the fragment.
        // This makes the fragment self-contained but less reusable.
        // A better pattern is to have the hosting Activity manage the service connection and
        // provide the service instance to the fragment. This will be refactored later.
        try {
            // Start the {@link TermuxService} and make it run regardless of who is bound to it
            Intent serviceIntent = new Intent(requireActivity(), TermuxService.class);
            requireActivity().startService(serviceIntent);

            // Attempt to bind to the service, this will call the {@link
            // #onServiceConnected(ComponentName, IBinder)}
            // callback if it succeeds.
            if (!requireActivity().bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "TermuxFragment failed to start TermuxService", e);
            Logger.showToast(requireActivity(),
                    getString(e.getMessage() != null && e.getMessage().contains("app is in background") ? R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),
                    true);
            mIsInvalidState = true;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable
                    ViewGroup container, @Nullable Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onCreateView");
        mRootView = bindLayout(inflater, container);
        return mRootView;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.logVerbose(LOG_TAG, "onViewCreated");

        if (mIsInvalidState) {
            requireActivity().finish();
            return;
        }

        // Bind specific UI controls as requested
        mTermuxActivityRootView = view.findViewById(R.id.activity_termux_root_view);
        mTermuxActivityBottomSpaceView = view.findViewById(R.id.activity_termux_bottom_space_view);

        // Set reference to fragment in root view. 
        // This allows TermuxActivityRootView to access fragment state if needed.
        if (mTermuxActivityRootView != null) {
             mTermuxActivityRootView.setFragment(this);
        }

        mTabsView = view.findViewById(R.id.terminal_tabs_view);
        mSessionTabManager = new SessionTabManager(requireContext());

        // Set default working directory to manager
        if (mInitialWorkingDir != null) {
            mSessionTabManager.updateWorkingDirectory(mInitialWorkingDir);
        }
        
        // Setup Bottom Sheet logic for the new Drawer-style toolbar
        setupBottomSheet(view);

        // setMargins();
        // ZeroKeyboardInsetsLayout automatically handles insets, so we don't strictly need manual margin setting here
        // but we keep the listener for legacy compatibility if needed.
        View content = requireActivity().findViewById(android.R.id.content);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                mNavBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                return insets;
            });
        }

        if (mProperties.isUsingFullScreen()) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setTermuxTerminalViewAndClients();

        // Initialize the toolbar logic (Extra Keys + Text Input) inside the Bottom Sheet
        setTerminalToolbarView(savedInstanceState);

        registerForContextMenu(mTerminalView);

        FileReceiverActivity.updateFileReceiverActivityComponentsState(requireActivity());

        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that
        // Termux
        // app has been opened.
        TermuxUtils.sendTermuxOpenedBroadcast(requireActivity());


        view.post(() -> {
            if (mTerminalView != null) {
                mTerminalView.requestFocus();
                // Ensure keyboard doesn't get stuck in a weird state
                if (!mPreferences.shouldSoftKeyboardBeHiddenOnStartup()) {
                    KeyboardUtils.showSoftKeyboard(requireActivity(), mTerminalView);
                }
            }
        });
    }
    
    /**
     * Initializes the BottomSheet logic for the toolbar.
     * Binds the drag handle and sets up the behavior.
     */
    private void setupBottomSheet(View rootView) {
        mBottomSheetContainer = rootView.findViewById(R.id.bottom_sheet_container);
        mDragHandleView = rootView.findViewById(R.id.drag_handle_view);

        if (mBottomSheetContainer != null) {
            mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetContainer);
            
            // Initial state: based on user preference
            if (mPreferences.shouldShowTerminalToolbar()) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            // Handle click on the drag handle to toggle state
            // Ripple effect is handled by XML foreground drawable
            if (mDragHandleView != null) {
                mDragHandleView.setOnTouchListener(new DragHandleTouchListener());
            }

            // Optional: Listen to callbacks to update preferences or UI state
            mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        // Toolbar is fully visible
                        mPreferences.setShowTerminalToolbar(true);

                        if (mTerminalView != null) mTerminalView.requestFocus();
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        mPreferences.setShowTerminalToolbar(false);
                        if (mTerminalView != null) mTerminalView.requestFocus();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            });
        }
    }

    /**
     * Touch Listener for the Floating Drag Handle (Three Dots)
     * Handles Click (Toggle) and Horizontal Dragging
     */
    private class DragHandleTouchListener implements View.OnTouchListener {
        private float dX;
        private boolean isDragging = false;
        private final GestureDetector gestureDetector;

        public DragHandleTouchListener() {
            gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    // Handle Click: Toggle Drawer
                    toggleTerminalToolbar();
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    // Enable dragging mode on Long Press
                    isDragging = true;
                    // Haptic feedback
                    mDragHandleView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    // Optional: visual feedback for drag mode
                    mDragHandleView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
                }
            });
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - event.getRawX();
                    isDragging = false; // Wait for long press to enable drag
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        // Move Horizontally only
                        float newX = event.getRawX() + dX;
                        
                        // Clamp to screen bounds
                        float parentWidth = ((View) view.getParent()).getWidth();
                        if (newX < 0) newX = 0;
                        if (newX + view.getWidth() > parentWidth) newX = parentWidth - view.getWidth();

                        view.animate()
                                .x(newX)
                                .setDuration(0)
                                .start();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging) {
                        // Restore scale
                        mDragHandleView.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    }
                    isDragging = false;
                    return true;
            }
            return false;
        }
    }

    /**
     * Toggles the Bottom Sheet state to show or hide (collapse) the toolbar.
     */
    public void toggleTerminalToolbar() {
        if (mBottomSheetBehavior == null) return;

        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            Logger.showToast(requireActivity(), getString(R.string.msg_disabling_terminal_toolbar), true);
        } else {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            Logger.showToast(requireActivity(), getString(R.string.msg_enabling_terminal_toolbar), true);
            
            // If showing and text input is selected, request focus
            if (isTerminalToolbarTextInputViewSelected()) {
                 View textInput = mRootView.findViewById(R.id.terminal_toolbar_text_input);
                 if(textInput != null) textInput.requestFocus();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();
        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.logVerbose(LOG_TAG, "onResume");

        if (mIsInvalidState) return;
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();
        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();
        

        if (mTerminalView != null) {
            mTerminalView.requestFocus();
        }

        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(requireActivity(), LOG_TAG);

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    public void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();
        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Logger.logDebug(LOG_TAG, "onDestroyView");

        if (mLastToast != null) {
            mLastToast.cancel();
            mLastToast = null;
        }
        if (mSessionTabManager != null) mSessionTabManager.detach();

        if (mIsInvalidState) return;
        if (mTermuxService != null) {
            mTermuxService.unsetTermuxTerminalSessionClient();
        }
        try {
            if (mTermuxService != null) {
                requireActivity().unbindService(this);
                mTermuxService = null;
            }
        } catch (Exception e) {
            // ignore.
        }

        // Clear view references to prevent leaks
        mRootView = null;
        mTermuxActivityRootView = null;
        mTermuxActivityBottomSpaceView = null;
        mExtraKeysView = null;
        mTabsView = null;
        
        mBottomSheetContainer = null;
        mDragHandleView = null;
        mBottomSheetBehavior = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }

    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with {@link
     * #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a
     * call to this callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Logger.logDebug(LOG_TAG, "onServiceConnected");

        mTermuxService = ((TermuxService.LocalBinder) service).service;

        mSessionTabManager.attachToFragment(this);
        mTabsView.attachToFragment(this, mSessionTabManager);

        final Intent intent = requireActivity().getIntent();
        requireActivity().setIntent(null);

        final String workingDir = intent != null && intent.getExtras() != null ? intent.getExtras().getString(TERMUX_ACTIVITY.EXTRA_SESSION_WORKING_DIR, null) : null;
        final String sessionName = intent != null && intent.getExtras() != null ? intent.getExtras().getString(TERMUX_ACTIVITY.EXTRA_SESSION_NAME, null) : null;
        boolean launchFailsafe = intent != null && intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);

        if (mTermuxService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                TermuxInstaller.setupBootstrapIfNeeded(requireActivity(), () -> {
                    if (mTermuxService == null) return;
                    try {
                        if (mTermuxService.isTermuxSessionsEmpty()) {
                            // Use project dir if available, else default
                            String cwd = mInitialWorkingDir != null ? mInitialWorkingDir : mSessionTabManager.defaultWorkingDirectory;
                            mSessionTabManager.createNewSession(launchFailsafe, sessionName, cwd);
                        }
                    } catch (WindowManager.BadTokenException e) {
                    }
                });
            } else {
                finishActivityIfNotFinishing();
            }
        } else {
            final Optional<
                    TermuxSession> existingSession = workingDir == null ? Optional.empty() : mTermuxService.getTermuxSessions().stream().filter(session -> Objects.equals(
                            session.getTerminalSession().getCwd(), workingDir)).findFirst();
            setupTermuxSessionOnServiceConnected(intent, workingDir, sessionName, existingSession.orElse(null), launchFailsafe);
        }
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);
    }

    protected void setupTermuxSessionOnServiceConnected(Intent intent, String workingDir, String sessionName, TermuxSession existingSession, boolean launchFailsafe) {
        if (mTermuxService.isTermuxSessionsEmpty()) {
            onCreateNewSession(launchFailsafe, sessionName, workingDir);
            return;
        }
        if (existingSession != null) {
            if (existingSession.getExecutionCommand().isFailsafe != launchFailsafe) {
                onCreateNewSession(launchFailsafe, sessionName, workingDir);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSession(existingSession.getTerminalSession());
            }
        } else if (workingDir != null) {
            onCreateNewSession(launchFailsafe, sessionName, workingDir);
        } else {
            mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
        }

        // Refresh tabs view
        mTabsView.refreshSessions();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        finishActivityIfNotFinishing();
    }

    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();
        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }

    private void setActivityTheme() {
        // Ensure properties are initialized. This check is important as this method is called
        // early.
        if (mProperties == null) {
            // This should have been initialized in onAttach, but as a safeguard:
            mProperties = TermuxAppSharedProperties.getProperties();
            if (mProperties == null) return;
        }

        // Update NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        // Get the host activity
        Activity activity = requireActivity();

        // Check if the hosting activity is an AppCompatActivity before attempting to use
        // AppCompat-specific features.
        if (activity instanceof AppCompatActivity) {
            // It is safe to cast and set the night mode now.
            // If NightMode.SYSTEM is set, then android will automatically
            // trigger recreation of activity when uiMode/dark mode configuration is changed so that
            // day or night theme takes affect.
            AppCompatActivityUtils.setNightMode((AppCompatActivity) activity, NightMode.getAppNightMode().getName(), true);
        } else {
            Logger.logWarn(LOG_TAG, "The hosting activity '" + activity.getClass().getName() + "' is not an AppCompatActivity. Dynamic night mode theming will not be applied.");
        }
    }

    private void setMargins() {
        RelativeLayout relativeLayout = mRootView.findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }

    private void setTermuxTerminalViewAndClients() {
        mTermuxTerminalSessionActivityClient = onCreateTerminalSessionClient();
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);
        mTerminalView = mRootView.findViewById(R.id.terminal_view);
        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);
        if (mTermuxTerminalViewClient != null) mTermuxTerminalViewClient.onCreate();
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    @NonNull
    protected TermuxTerminalSessionFragmentClient onCreateTerminalSessionClient() {
        return new TermuxTerminalSessionFragmentClient(this);
    }

    /**
     * Initialize and setup the bottom toolbar (ExtraKeys + Text Input).
     * The ViewPager is now hosted inside the BottomSheet layout in fragment_terminal.xml.
     */
    private void setTerminalToolbarView(Bundle savedInstanceState) {
        // Initialize the ExtraKeys logic
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView, mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);
        
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) {
            Logger.logError(LOG_TAG, "terminalToolbarViewPager is null!");
            return;
        }

        // We always keep it visible in the layout now, as show/hide is handled by BottomSheet collapsing/expanding
        terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        // Setup the ViewPager adapter which contains the extra keys and text input views
        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));
        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        // Calculate height based on number of rows in extra keys matrix
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
                (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
                mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    /**
     * Toggles the Bottom Sheet state to show or hide (collapse) the toolbar.
     // */
    // public void toggleTerminalToolbar() {
        // if (mBottomSheetBehavior == null) return;

        // if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            // mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            // Logger.showToast(requireActivity(), getString(R.string.msg_disabling_terminal_toolbar), true);
        // } else {
            // mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            // Logger.showToast(requireActivity(), getString(R.string.msg_enabling_terminal_toolbar), true);
            
            // // If showing and text input is selected, request focus
            // if (isTerminalToolbarTextInputViewSelected()) {
                 // View textInput = mRootView.findViewById(R.id.terminal_toolbar_text_input);
                 // if(textInput != null) textInput.requestFocus();
            // }
        // }
    // }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null || mRootView == null) return;
        final EditText textInputView = mRootView.findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty())
                savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }

    public void onCreateNewSession(boolean isFailsafe, String sessionName, String workingDirectory) {
        // Use default working directory if not provided
        if (workingDirectory == null) {
            workingDirectory = mInitialWorkingDir;
        }
        mTermuxTerminalSessionActivityClient.addNewSession(isFailsafe, sessionName, workingDirectory);
        if (mTabsView != null) mTabsView.refreshSessions();
    }

    public boolean onBackPressed() {
        return false;
    }

    public void finishActivityIfNotFinishing() {
        if (requireActivity() != null && !requireActivity().isFinishing()) {
            requireActivity().finish();
        }
    }

    /** Show a toast and dismiss the last one if still visible. */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(requireActivity(), text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean addAutoFillMenu = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = requireActivity().getSystemService(AutofillManager.class);
            if (autofillManager != null && autofillManager.isEnabled()) addAutoFillMenu = true;
        }
        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (addAutoFillMenu)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_ID, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mTerminalView.showContextMenu();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();
        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                mTermuxTerminalViewClient.showUrlSelection();
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                mTermuxTerminalViewClient.shareSessionTranscript();
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                mTermuxTerminalViewClient.shareSelectedText();
                return true;
            case CONTEXT_MENU_AUTOFILL_ID:
                requestAutoFill();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                onResetTerminalSession(session);
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                showKillSessionDialog(session);
                return true;
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:
                toggleKeepScreenOn();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(requireActivity(), new Intent(requireActivity(), HelpActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                mTermuxTerminalViewClient.reportIssueFromTranscript();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void onContextMenuClosed(Menu menu) {
        if (mTerminalView != null) mTerminalView.onContextMenuClosed(menu);
    }

    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;
        final AlertDialog.Builder b = new AlertDialog.Builder(requireActivity());
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            showToast(getResources().getString(R.string.msg_terminal_reset), true);
            if (mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void toggleKeepScreenOn() {
        if (mTerminalView.getKeepScreenOn()) {
            mTerminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            mTerminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }

    private void requestAutoFill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = requireActivity().getSystemService(AutofillManager.class);
            if (autofillManager != null && autofillManager.isEnabled()) {
                autofillManager.requestAutofill(mTerminalView);
            }
        }
    }

    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public ViewPager getTerminalToolbarViewPager() {
        if (mRootView == null) return null;
        return (ViewPager) mRootView.findViewById(R.id.terminal_toolbar_view_pager);
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 1;
    }

    public void termuxSessionListNotifyUpdated() {
           if (mTabsView != null) {
                mTabsView.refreshSessions();
             }
     }

    public boolean isVisibles() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }

    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionFragmentClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        return mTerminalView != null ? mTerminalView.getCurrentSession() : null;
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }

    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {
        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);
        context.sendBroadcast(stylingIntent);
    }

    private void reloadActivityStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();
            if (mExtraKeysView != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);
            }
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }
        setTerminalToolbarHeight();
        FileReceiverActivity.updateFileReceiverActivityComponentsState(requireActivity());
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();
        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadActivityStyling();
        if (recreateActivity) {
            requireActivity().recreate();
        }
    }

    public static TermuxFragment newInstance(@Nullable Bundle args) {
        TermuxFragment fragment = new TermuxFragment();
        if (args != null) fragment.setArguments(args);
        return fragment;
    }

    public static TermuxFragment newInstance() {
        return newInstance(null);
    }
}