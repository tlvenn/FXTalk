/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fxtalk;

import fxtalk.ui.notification.NotifBubble;
import fxtalk.ui.buddylist.PalListPanel;
import fxtalk.ui.login.LoginScreen;
import fxtalk.ui.login.LoadingPalListUI;
import fxtalk.ui.chat.AccordionChatLogPanel;
import fxtalk.ui.chat.ChatLogPanel;
import fxtalk.ui.settings.SettingsPanel;
import fxtalk.network.ConnectionHandle;
import fxtalk.ui.buddylist.AddSearchComponent;
import fxtalk.ui.buddylist.SelfComponent;
import fxtalk.utils.TalkSettings;
import fxtalk.ui.misc.SplashScreen;
import fxtalk.ui.misc.UIutil;
import fxtalk.utils.Util;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Main app, creates 
 * main stage (contains list of buddies), 
 * LoginScreen stage
 * settings stage
 * chatLogPanel stage
 * notificbubble stage
 * @author srikalyc
 */
public class FXTalkApp extends Application {
    public static double WIDTH = 305;
    public static double HEIGHT = 456;
    
    
    public ConnectionHandle conn;
    public SplashScreen splashScreen;
    public LoginScreen loginScreen;
    public String currentPal;//The one with whom the current chat is in progress.
    public Stage primaryStage;
    public Scene scene;
    public VBox base = null;
    public SettingsPanel settingsPanel = null;
    public PalListPanel palListPanel = null;//This is not a stage mind it.
    public LoadingPalListUI  loadingPalListUI = null;
    public ChatLogPanel chatLogPanel = null;
    public NotifBubble notifPanel = null;
    
    public SelfComponent selfComponent = null;
    public AddSearchComponent addSearchComponent = null;
    
    public ObjectProperty<AppState> appState = new SimpleObjectProperty<AppState>(AppState.NOT_AUTHENTICATED);
    
    public boolean startShowingPresenceNotifics = false;//When user logs out he resets this to false and when login succeeds this will be set to true after  3seconds.
    boolean byPassSplashScreen = true;
    static final int SPLASH_SCREEN_SHOW_TIME = 3000;
    public static FXTalkApp app = null;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStg) {
        app = this;
        this.primaryStage = primaryStg;
        primaryStage.setTitle("FX-Talk");
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        
        splashScreen = new SplashScreen();
        splashScreen.initialize();
        if (!byPassSplashScreen) {
            splashScreen.show();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!byPassSplashScreen) {
                    Util.gotoSleep(SPLASH_SCREEN_SHOW_TIME);
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        postUIInit();
                    }
                });
            }
        }, "FXTalkApp.SplashScreenThread").start();

    }
/**
 * Initialize the UI after the initial splash screen is shown
 */
    private void postUIInit() {
        if (!byPassSplashScreen) { //We did not show splash screen, so no need to hide it.
            splashScreen.hide();
        }
//////.//////////Set the stage for all the stages(NOTE:Some are not stages!)////////////////
        loginScreen = new LoginScreen();
        settingsPanel = new SettingsPanel();
        chatLogPanel = new AccordionChatLogPanel();
        
        selfComponent = new SelfComponent();
        palListPanel = new PalListPanel();
        addSearchComponent = new AddSearchComponent();
        loadingPalListUI = new LoadingPalListUI();
        notifPanel = new NotifBubble();

/////////////Setup z-ordering(Not exactly but would be nice if JavaFX enforced the z-order)/////
        loginScreen.initOwner(primaryStage);
        settingsPanel.initOwner(primaryStage);
        

//////.//////////Initialize all the stages////////////////
        loginScreen.initialize();// will take care of establishing connection to the server.
        notifPanel.initialize();
        settingsPanel.initialize();

        settingsPanel.show();
////////////////////////Post initialization///////////////
        settingsPanel.setX(loginScreen.getX());
        settingsPanel.setY(loginScreen.getY() - 23);
        settingsPanel.postInit();
        
        primaryStage.setX(settingsPanel.getX());
        primaryStage.setY(settingsPanel.getY() + 23);
        
        base = VBoxBuilder.create().spacing(0).alignment(Pos.TOP_CENTER).children(addSearchComponent, palListPanel, selfComponent).visible(false).build();
        base.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                FXTalkApp.app.settingsPanel.toFront();
                FXTalkApp.app.chatLogPanel.toFront();
                FXTalkApp.app.primaryStage.toFront();
            }
        });
        
        
        scene = SceneBuilder.create().root(base).width(WIDTH).height(HEIGHT).fill(Color.TRANSPARENT).build();

        //ScenicView.show(scene);

        primaryStage.setScene(scene);
        //ScenicView.show(scene);
        scene.getStylesheets().add(UIutil.getRsrcURI("fxtalk.css").toString());
        
/////Following is needed because say you have switched to another app and have come
        //back to FXTalk app using Alt-Tab(OR) Cmd-Tab then all the stages like
        //settingsPanel, chatLogPanel etc must also be visible along with main stage.
        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (primaryStage.isFocused()) {
                    settingsPanel.show();
                    if (chatLogPanel.isShoing) {
                        chatLogPanel.show();
                    }
                }
            }
        });
        loginScreen.toFront();
        loginScreen.gainFocus();

        TalkSettings.hookupActivityTrackerForStage(primaryStage);
        TalkSettings.hookupActivityTrackerForStage(settingsPanel);
        TalkSettings.hookupActivityTrackerForStage(chatLogPanel);
        
        TalkSettings.proxyChanged.addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldV, Boolean newV) {
                if (newV && !loginScreen.isShowing()) {//If login screen is still shown then we need not call reLoginWithChangedProxy, just wait for user action.
                    TalkSettings.setProxyProperties();
                    reLoginWithChangedProxy();
                }
            }
        });
        
        appState.addListener(new ChangeListener<AppState>() {
            @Override
            public void changed(ObservableValue<? extends AppState> ov, AppState oldVal, AppState newVal) {
                if (newVal == FXTalkApp.AppState.CONNECTION_TIMED_OUT) {
                    loginScreen.statusText.setText("Connection Timedout");
                } else if (newVal == FXTalkApp.AppState.NETWORK_ISSUE) {
                    loginScreen.statusText.setText("Tune Network Settings");
                } else if (newVal == FXTalkApp.AppState.INVALID_CREDENTIALS) {
                    loginScreen.statusText.setText("Authentication failed!");
                } else if (newVal == FXTalkApp.AppState.AUTHENTICATED) {
                    loginScreen.statusText.setText("Authentication Succeeds!");
                } else if (newVal == FXTalkApp.AppState.AUTH_IN_PROGRESS) {
                    loginScreen.statusText.setText("Authenticating..");
                } else if (newVal == FXTalkApp.AppState.NOT_AUTHENTICATED) {
                    loginScreen.statusText.setText("Login to your Account");
                } 
            }
        });
        
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/fxtalk/assets/FXTalk.png")));
        loginScreen.getIcons().add(new Image(this.getClass().getResourceAsStream("/fxtalk/assets/FXTalk.png")));
        settingsPanel.getIcons().add(new Image(this.getClass().getResourceAsStream("/fxtalk/assets/FXTalk.png")));
        notifPanel.dummyStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/fxtalk/assets/FXTalk.png")));
    }
/**
 * After the connection and login succeeds this method must be called ..
 */
    public void loginSuccessful() {
        
        loginScreen.hide();
        primaryStage.show();
        //TODO: Not needed
        //loadingPalListUI.service();
        //TODO: Uncomment the following to see any issues with node visibility, layout issues etc.
        //ScenicView.show(scene);
        settingsPanel.postInit();
        
        settingsPanel.toFront();
        primaryStage.toFront();
        base.setVisible(true);
      
        selfComponent.loadPresenceOrSetDefault();
        selfComponent.loadStatusOrSetDefault();
        selfComponent.loadAvatarOrSetDefault();
        selfComponent.loadSignoutComponentOrSetDefault();
        
        app.appState.set(AppState.AUTHENTICATED);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                Util.gotoSleep(3000);//Wait for 3 seconds and then enable notifications(that too only if user has explicitly asked)
                ///We set the startShowingPresenceNotifics to true here because if it were true
                //from the beginning then we will be overwhelmed by notifications during sign in process.
                startShowingPresenceNotifics = true;
            }
        }, "FXTalkApp.StartShowingPresenceNotifications").start();
    }
/**
 * After proxy settings are changed this method must be called.
 */
    private void reLoginWithChangedProxy() {
        new Thread(new Runnable() {//Long running task hence put this in thread.

            @Override
            public void run() {
                conn.tearDownConnection();
                Platform.runLater(new Runnable() {//This is a UI operation outside JavaFX Thread hence ..

                    @Override
                    public void run() {
                        chatLogPanel.resetPanel();
                        palListPanel.resetPanel();
                        //loadingPalListUI.service();
                        final boolean isAuthenticated = conn.createConnection();
                        if (isAuthenticated) {
                            selfComponent.loadPresenceOrSetDefault();
                        }
                        TalkSettings.proxyChanged.set(false);//Reset back to false so that when proxy is changed again this can be set to true.
                    }
                });
            }
        }, "FXTalkApp.reloginWithChangedProxy").start();

    }
    public enum AppState {
        AUTH_IN_PROGRESS,
        NOT_AUTHENTICATED,
        AUTHENTICATED,
        INVALID_CREDENTIALS,
        CONNECTION_TIMED_OUT,
        CONNECTION_CLOSED_BY_CLIENT,
        CONNECTION_CLOSED_BY_SERVER,
        PROXY_CHANGED,
        NETWORK_ISSUE,
        FILE_DOWNLOAD_IN_PROGRESS,
        FILE_UPLOAD_IN_PROGRESS,
        VOICE_CALL_IN_PROGRESS
    }
}
