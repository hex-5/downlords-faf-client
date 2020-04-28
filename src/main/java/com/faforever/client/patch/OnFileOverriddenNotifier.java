package com.faforever.client.patch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.patch.event.FileOverriddenEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.scene.image.Image;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;

@Component
public class OnFileOverriddenNotifier implements InitializingBean
{
  private final PlatformService platformService;
  private final ExecutorService executorService;
  private final NotificationService notificationService;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private String faWindowTitle;
  @Inject
  public OnFileOverriddenNotifier(PlatformService platformService, ExecutorService executorService,
                            NotificationService notificationService,
                            EventBus eventBus,
                            ClientProperties clientProperties) {
    this.platformService = platformService;
    this.executorService = executorService;
    this.notificationService = notificationService;
    this.eventBus = eventBus;
    this.clientProperties = clientProperties;
    this.faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    this.image = new Image("/images/dopefish_lives.gif");
  }
  private Image image;
  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onFileOverriden(FileOverriddenEvent event) {
    notificationService.addNotification(new TransientNotification("YOUR ATTENTION IS REQUIRED!", "The File \"" + event.getModFile().getName() + "\" was updated. Please look into this.",
        this.image,
        v -> platformService.focusWindow(faWindowTitle)));
    notificationService.addNotification(new PersistentNotification("The File \"" + event.getModFile().getName() + "\" was updated. Please look into this.",
        Severity.WARN));
  }
}
