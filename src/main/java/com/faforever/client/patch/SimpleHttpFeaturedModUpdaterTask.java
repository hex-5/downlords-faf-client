package com.faforever.client.patch;

import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.DownloadService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.patch.event.FileOverriddenEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.UpdaterUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import javafx.scene.control.Alert;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SimpleHttpFeaturedModUpdaterTask extends CompletableTask<PatchResult> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final DownloadService downloadService;
  private final I18n i18n;

  private FeaturedMod featuredMod;
  private Integer version;
  private final EventBus eventBus;

  public SimpleHttpFeaturedModUpdaterTask(FafService fafService, PreferencesService preferencesService, DownloadService downloadService, I18n i18n, EventBus eventBus) {
    super(Priority.HIGH);

    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.downloadService = downloadService;
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  @Override
  protected PatchResult call() throws Exception {
    String initFileName = "init_" + featuredMod.getTechnicalName() + ".lua";

    updateTitle(i18n.get("updater.taskTitle"));
    updateMessage(i18n.get("updater.readingFileList"));

    List<FeaturedModFile> featuredModFiles = fafService.getFeaturedModFiles(featuredMod, version).get();

    Path initFile = null;
    for (FeaturedModFile featuredModFile : featuredModFiles) {
      Path fafDataDirectory = preferencesService.getFafDataDirectory();
      Path targetPath = fafDataDirectory
          .resolve(featuredModFile.getGroup())
          .resolve(featuredModFile.getName());

      String sMd5 = "";
      String sMd5Server = featuredModFile.getMd5();

      if(Files.exists(targetPath))
      {
        sMd5 = com.google.common.io.Files.hash(targetPath.toFile(), Hashing.md5()).toString();
        //check fails anyways if md5 is empty
        if(sMd5Server.equals(sMd5))
        {
          logger.debug("Already up to date: {}", targetPath);
        }
        else
        {
          logger.debug("Maybe not up to date or modified: {}", targetPath);
          logger.debug("Please consider to check this file by yourself: {}", targetPath);
          updateMessage("files go brrrrrrrrrrrrrrr!");
          Path targetPathOriginal = fafDataDirectory
              .resolve(featuredModFile.getGroup())
              .resolve(featuredModFile.getName()+".original");
          if(Files.exists(targetPathOriginal))
          {
            String sMd5Original = com.google.common.io.Files.hash(targetPathOriginal.toFile(), Hashing.md5()).toString();
            if(!sMd5Original.equals(sMd5Server))
            {
              downloadUpdateFile(featuredModFile, fafDataDirectory, targetPath);

              eventBus.post(new FileOverriddenEvent(featuredModFile));
            }
          }
        }
      }
      else
      {
        downloadUpdateFile(featuredModFile, fafDataDirectory, targetPath);
      }


      if ("bin".equals(featuredModFile.getGroup()) && initFileName.equalsIgnoreCase(featuredModFile.getName())) {
        initFile = targetPath;
      }
    }

    Assert.isTrue(initFile != null && Files.exists(initFile), "'" + initFileName + "' could be found.");

    int maxVersion = featuredModFiles.stream()
        .mapToInt(mod -> Integer.parseInt(mod.getVersion()))
        .max()
        .orElseThrow(() -> new IllegalStateException("No version found"));

    return PatchResult.withLegacyInitFile(new ComparableVersion(String.valueOf(maxVersion)), initFile);
  }

  private void downloadUpdateFile(FeaturedModFile featuredModFile, Path fafDataDirectory, Path targetPath) throws IOException {
    Files.createDirectories(targetPath.getParent());
    updateMessage(i18n.get("updater.downloadingFile", targetPath.getFileName()));

    String url = featuredModFile.getUrl();
    downloadService.downloadFile(new URL(url), targetPath, this::updateProgress);
    UpdaterUtil.extractMoviesIfPresent(targetPath, fafDataDirectory);
  }

  public void setFeaturedMod(FeaturedMod featuredMod) {
    this.featuredMod = featuredMod;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
