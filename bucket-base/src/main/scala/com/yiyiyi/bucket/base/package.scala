package com.yiyiyi.bucket

import java.sql.Timestamp
import java.text.{ DecimalFormat, SimpleDateFormat }

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
 * @author xuejiao
 */
package object base {
  val zeroTimestamp = new Timestamp(0)
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val decimalFormat = new DecimalFormat("#.####")

  val objectMapper = new ObjectMapper() with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.setSerializationInclusion(Include.NON_ABSENT)
  objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
  objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}
