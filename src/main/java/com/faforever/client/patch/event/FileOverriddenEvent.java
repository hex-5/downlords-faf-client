package com.faforever.client.patch.event;

import com.faforever.client.api.dto.FeaturedModFile;

public class FileOverriddenEvent {
  public FileOverriddenEvent(FeaturedModFile featuredModFile) {
    m_modfile = featuredModFile;
  }
  private FeaturedModFile m_modfile;
  public FeaturedModFile getModFile()
  {
    return m_modfile;
  }
}
