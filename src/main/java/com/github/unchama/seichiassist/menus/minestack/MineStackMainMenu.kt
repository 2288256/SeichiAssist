package com.github.unchama.seichiassist.menus.minestack

import arrow.core.Left
import com.github.unchama.itemstackbuilder.IconItemStackBuilder
import com.github.unchama.menuinventory.IndexedSlotLayout
import com.github.unchama.menuinventory.MenuInventoryView
import com.github.unchama.menuinventory.slot.button.Button
import com.github.unchama.menuinventory.slot.button.action.ClickEventFilter
import com.github.unchama.menuinventory.slot.button.action.FilteredButtonEffect
import com.github.unchama.seichiassist.SeichiAssist
import com.github.unchama.seichiassist.menus.CommonButtons
import com.github.unchama.seichiassist.minestack.MineStackObjectCategory
import com.github.unchama.seichiassist.minestack.MineStackObjectCategory.*
import com.github.unchama.targetedeffect.*
import com.github.unchama.targetedeffect.player.FocusedSoundEffect
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

object MineStackMainMenu {
  private object ButtonComputations {
    val categoryButtonLayout = run {
      fun iconMaterialFor(category: MineStackObjectCategory): Material = when (category) {
        ORES -> Material.DIAMOND_ORE
        MOB_DROP -> Material.ENDER_PEARL
        AGRICULTURAL -> Material.SEEDS
        BUILDING -> Material.SMOOTH_BRICK
        REDSTONE_AND_TRANSPORTATION -> Material.REDSTONE
        GACHA_PRIZES -> Material.GOLDEN_APPLE
      }

      fun labelFor(category: MineStackObjectCategory): String = when (category) {
        ORES -> "鉱石系アイテム"
        MOB_DROP -> "ドロップ系アイテム"
        AGRICULTURAL -> "農業・食料系アイテム"
        BUILDING -> "建築系アイテム"
        REDSTONE_AND_TRANSPORTATION -> "レッドストーン・移動系アイテム"
        GACHA_PRIZES -> "ガチャ品"
      }

      val layoutMap = MineStackObjectCategory.values().mapIndexed { index, category ->
        val slotIndex = index + 1 // 0には自動スタック機能トグルが入るので、1から入れ始める
        val iconItemStack = IconItemStackBuilder(iconMaterialFor(category))
            .lore(listOf("$BLUE$UNDERLINE$BOLD${labelFor(category)}"))
            .build()

        slotIndex to Button(iconItemStack) // TODO クリックで各カテゴリのUIを開く
      }.toMap()

      IndexedSlotLayout(layoutMap)
    }

    suspend fun Player.computeAutoMineStackToggleButton(): Button {
      val playerData = SeichiAssist.playermap[uniqueId]!!

      val iconItemStack = run {
        val baseBuilder =
            IconItemStackBuilder(Material.IRON_PICKAXE)
                .title("$YELLOW$UNDERLINE${BOLD}対象ブロック自動スタック機能")

        if (playerData.minestackflag) {
          baseBuilder
              .enchanted()
              .lore(listOf(
                  "$RESET${GREEN}現在ONです",
                  "$RESET$DARK_RED${UNDERLINE}クリックでOFF"
              ))
        } else {
          baseBuilder
              .lore(listOf(
                  "$RESET${RED}現在OFFです",
                  "$RESET$DARK_GREEN${UNDERLINE}クリックでON"
              ))
        }.build()
      }

      val buttonEffect = FilteredButtonEffect(ClickEventFilter.ALWAYS_INVOKE) {
        sequentialEffect(
            playerData.toggleAutoMineStack(),
            deferredEffect {
              val message: String
              val soundPitch: Float
              when {
                playerData.minestackflag -> {
                  message = "${GREEN}対象ブロック自動スタック機能:ON"
                  soundPitch = 1.0f
                }
                else -> {
                  message = "${RED}対象ブロック自動スタック機能:OFF"
                  soundPitch = 0.5f
                }
              }

              sequentialEffect(
                  message.asMessageEffect(),
                  FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, soundPitch)
              )
            },
            deferredEffect { overwriteCurrentSlotBy(computeAutoMineStackToggleButton()) }
        )
      }

      return Button(iconItemStack, buttonEffect)
    }

    /**
     * メインメニュー内の「履歴」機能部分のレイアウトを計算する
     */
    suspend fun Player.computeHistoricalMineStackLayout(): IndexedSlotLayout {
      val playerData = SeichiAssist.playermap[uniqueId]!!

      val buttonMapping = playerData.hisotryData.usageHistory.mapIndexed { index, mineStackObject ->
        val slotIndex = 18 + index // 3行目から入れだす
        val button = with(MineStackButtons) { getMineStackItemButtonOf(mineStackObject) }

        slotIndex to button
      }.toMap()

      return IndexedSlotLayout(buttonMapping)
    }
  }

  private suspend fun Player.computeMineStackMainMenuLayout(): IndexedSlotLayout {
    return with(ButtonComputations) {
      IndexedSlotLayout(
          0 to computeAutoMineStackToggleButton(),
          45 to CommonButtons.openStickMenu
      )
          .merge(categoryButtonLayout)
          .merge(computeHistoricalMineStackLayout())
    }
  }

  val openMainMenu: TargetedEffect<Player> = computedEffect { player ->
    val view = MenuInventoryView(
        Left(4 * 9),
        "$DARK_PURPLE${BOLD}MineStackメインメニュー",
        player.computeMineStackMainMenuLayout()
    )

    view.createNewSession().open
  }
}