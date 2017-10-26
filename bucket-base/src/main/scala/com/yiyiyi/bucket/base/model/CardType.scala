package com.yiyiyi.bucket.base.model

/**
 * @author xuejiao
 */
object CardType extends Enumeration {
  type CardType = Value
  val unknown, dizhu, jinghua, banzi = Value

  val playerNumLimit = Map(
    unknown -> 0,
    dizhu -> 4,
    jinghua -> -1, // -1 表示可以随意的人数
    banzi -> 4
  )
}

object CardAction extends Enumeration {
  type CardAction = Value
  val shuffle = Value //洗牌
  val cut = Value // 切牌
  val deal = Value //发牌
  val sort = Value // 理牌
  val draw = Value // 摸牌
  val play = Value // 打出
  val discard = Value //弃牌
}

object RoomStatus extends Enumeration {
  type RoomStatus = Value
  val off = Value(0)
  val on = Value(1)
}