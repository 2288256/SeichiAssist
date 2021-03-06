package com.github.unchama.itemmigration

import cats.effect.{ConcurrentEffect, ContextShift, SyncEffect}
import com.github.unchama.generic.ContextCoercion
import com.github.unchama.generic.effect.unsafe.EffectEnvironment
import com.github.unchama.itemmigration.bukkit.controllers.player.{PlayerItemMigrationController, PlayerItemMigrationStateRepository}
import com.github.unchama.itemmigration.bukkit.targets.PlayerInventoriesData
import com.github.unchama.itemmigration.domain.ItemMigrations
import com.github.unchama.itemmigration.service.ItemMigrationService

/**
 * プレーヤーのインベントリのマイグレーションを行うために必要なリスナー等のオブジェクトを提供するオブジェクトのクラス。
 */
class PlayerItemMigrationEntryPoints[
  F[_] : ConcurrentEffect : ContextShift,
  G[_] : SyncEffect : ContextCoercion[*[_], F]
](migrations: ItemMigrations, service: ItemMigrationService[F, PlayerInventoriesData[F]])
 (implicit effectEnvironment: EffectEnvironment) {

  private val repository = new PlayerItemMigrationStateRepository[F, G, F]
  private val controller = new PlayerItemMigrationController[F, G](repository, migrations, service)

  val listenersToBeRegistered = Seq(
    repository,
    controller
  )

}
