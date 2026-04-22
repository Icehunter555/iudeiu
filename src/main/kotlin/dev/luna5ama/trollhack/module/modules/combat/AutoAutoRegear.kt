package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.Kit
import dev.luna5ama.trollhack.util.Bind
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemShulkerBox
import org.lwjgl.input.Keyboard

internal object AutoAutoRegear : Module(
    name = "Auto Auto Regear",
    description = "Automatically use AutoRegear when player is low on items",
    category = Category.COMBAT
) {
    private val regearKey by setting("Regear Key", Bind())
    private val checkDelay by setting("Check Delay", 1000, 100..5000, 100)
    private val minItems by setting("Min Items", 5, 1..36, 1)
    private val checkArmor by setting("Check Armor", true)
    private val checkHotbar by setting("Check Hotbar", true)
    
    private val timer = TickTimer()
    private var lastRegearTime = 0L
    
    override fun getHudInfo(): String {
        return Kit.kitName
    }
    
    init {
        onDisable {
            timer.reset()
            lastRegearTime = 0L
        }
        
        safeListener<TickEvent.Post> {
            if (!timer.tick(checkDelay)) return@safeListener
            if (System.currentTimeMillis() - lastRegearTime < 5000L) return@safeListener
            
            if (shouldRegear()) {
                pressRegearKey()
                lastRegearTime = System.currentTimeMillis()
            }
        }
    }
    
    private fun SafeClientEvent.shouldRegear(): Boolean {
        val kitItems = Kit.getKitItemArray() ?: return false
        
        var missingCount = 0
        
        // Check hotbar
        if (checkHotbar) {
            for (i in 0 until 9) {
                val slot = player.inventory.getStackInSlot(i)
                val kitItem = kitItems.getOrNull(i + 27) ?: continue
                
                if (slot.isEmpty || slot.item != kitItem.item) {
                    missingCount++
                }
            }
        }
        
        // Check armor
        if (checkArmor) {
            for (i in 0 until 4) {
                val armorSlot = player.inventory.armorInventory[i]
                if (armorSlot.isEmpty) {
                    missingCount++
                }
            }
        }
        
        // Check inventory for essential items
        val inventoryItems = player.inventory.mainInventory.count { !it.isEmpty }
        
        return missingCount >= minItems || inventoryItems < minItems
    }
    
    private fun pressRegearKey() {
        if (regearKey.isEmpty) return
        
        val keyCode = regearKey.key.value
        if (keyCode == Keyboard.KEY_NONE) return
        
        try {
            val keyBinding = KeyBinding.getKeybinds().firstOrNull { it.keyCode == keyCode }
            keyBinding?.let {
                KeyBinding.setKeyBindState(keyCode, true)
                KeyBinding.onTick(keyCode)
                KeyBinding.setKeyBindState(keyCode, false)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
