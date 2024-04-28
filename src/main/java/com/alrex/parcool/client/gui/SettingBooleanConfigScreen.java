package com.alrex.parcool.client.gui;

import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.common.info.ActionInfo;
import com.alrex.parcool.common.info.ClientSetting;
import com.alrex.parcool.common.network.SyncClientInformationMessage;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Collections;

public class SettingBooleanConfigScreen extends ParCoolSettingScreen {
    private final ParCoolConfig.Client.Booleans[] booleans = ParCoolConfig.Client.Booleans.values();
    private final Checkbox[] configButtons = new Checkbox[booleans.length];

    public SettingBooleanConfigScreen(Component titleIn, ActionInfo info, ColorTheme theme) {
        super(titleIn, info, theme);
        currentScreen = 1;
        for (int i = 0; i < booleans.length; i++) {
            configButtons[i] = new Checkbox(0, 0, 0, Checkbox_Item_Height, Component.translatable(booleans[i].Path), booleans[i].get());
        }
    }

    @Override
    protected boolean isDownScrollable() {
        return topIndex + viewableItemCount < configButtons.length;
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int topOffset, int bottomOffset) {
        final int offsetX = 40;
        int contentWidth = width - offsetX * 2;
        int contentHeight = height - topOffset - bottomOffset;
        viewableItemCount = contentHeight / Checkbox_Item_Height;
        for (Checkbox configButton : configButtons) {
            configButton.setWidth(0);
        }
        for (int i = 0; i < viewableItemCount && i + topIndex < booleans.length; i++) {
            Checkbox button = configButtons[i + topIndex];
            button.setX(offsetX + 1);
            button.setY(topOffset + Checkbox_Item_Height * i);
            button.setWidth(contentWidth);
            button.setHeight(20);
            button.render(graphics, mouseX, mouseY, partialTick);
            graphics.fill(offsetX, button.getY() + button.getHeight(), width - offsetX, button.getY() + button.getHeight() + 1, color.getSubSeparator());
            String comment = booleans[i + topIndex].Comment;
            if (comment != null && button.getX() < mouseX && mouseX < button.getX() + contentWidth && button.getY() < mouseY && mouseY < button.getY() + 20) {
                graphics.renderComponentTooltip(
                        font,
                        Collections.singletonList(Component.literal(comment)),
                        mouseX, mouseY);
            }
        }
        graphics.fill(width - offsetX, topOffset, width - offsetX - 1, topOffset + contentHeight, color.getSeparator());
        graphics.fill(offsetX, topOffset, offsetX + 1, topOffset + contentHeight, color.getSeparator());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int type) {
        for (Checkbox button : configButtons) {
            if (button.mouseClicked(mouseX, mouseY, type)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, type);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    protected void save() {
        for (int i = 0; i < booleans.length; i++) {
            booleans[i].set(configButtons[i].selected());
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Parkourability parkourability = Parkourability.get(player);
        if (parkourability == null) return;
        parkourability.getActionInfo().setClientSetting(ClientSetting.readFromLocalConfig());
        SyncClientInformationMessage.sync(player, true);
    }
}
