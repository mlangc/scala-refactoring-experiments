package com.github.mlangc.experiments.refactoring

import com.google.common.io.Resources
import org.apache.commons.io.IOUtils

trait LocalResourceSupport {
  def localResourceAsString(filename: String): String = {
    IOUtils.toString(Resources.getResource(getClass, filename))
  }
}
