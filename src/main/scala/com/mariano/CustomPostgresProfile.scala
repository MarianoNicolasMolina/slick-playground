package com.mariano

import com.github.tminglei.slickpg.utils.SimpleArrayUtils
import com.github.tminglei.slickpg.{ExPostgresProfile, PgArraySupport, PgHStoreSupport, PgJsonSupport, PgPlayJsonSupport}
import play.api.libs.json.{JsValue, Json}

trait CustomPostgresProfile extends ExPostgresProfile with PgArraySupport
  with PgHStoreSupport with PgJsonSupport with PgPlayJsonSupport {

  override def pgjson: String = "jsonb"
  override val api =CustomPostgresAPI

  object CustomPostgresAPI extends API with ArrayImplicits with HStoreImplicits
  with JsonImplicits {
    implicit val stringListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper = new AdvancedArrayJdbcType[JsValue](pgjson,
      string => SimpleArrayUtils.fromString(Json.parse)(string).orNull,
      value => SimpleArrayUtils.mkString[JsValue](_.toString)(value)
    ) .to(_.toList)

  }
}

object CustomPostgresProfile extends CustomPostgresProfile
