package one.pouekdev.coordinatelist;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.compress.utils.Lists;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CListWaypointScreen extends Screen {
    public CListWaypointScreen(Text title) {
        super(title);
    }
    public ScrollList list;
    public int selected_waypoint_id = -1;
    public ButtonWidget copy_coordinates_button;
    public ButtonWidget edit_waypoint_button;
    @Override
    protected void init() {
        GridWidget gridWidget = new GridWidget();
        GridWidget gridWidgetBottom = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 4, 4, 0);
        gridWidgetBottom.getMainPositioner().margin(4, 4, 4, 0);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        GridWidget.Adder adderBottom = gridWidgetBottom.createAdder(2);
        adder.add(ButtonWidget.builder(Text.translatable("buttons.add.new.waypoint"), button -> {
            PlayerEntity player = CListVariables.minecraft_client.player;
            CListClient.addNewWaypoint((int) Math.round(player.getX()), (int) Math.round(player.getY()), (int) Math.round(player.getZ()),false);
            list.RefreshElements();
        }).width(300).build(),2, gridWidget.copyPositioner().marginTop(10));
        copy_coordinates_button = ButtonWidget.builder(Text.literal("---"), button -> {
            long window = CListVariables.minecraft_client.getWindow().getHandle();
            CListWaypoint waypoint = CListClient.variables.waypoints.get(selected_waypoint_id);
            GLFW.glfwSetClipboardString(window, waypoint.x + " " + waypoint.y + " " + waypoint.z);
        }).width(150).build();
        copy_coordinates_button.setTooltip(Tooltip.of(Text.translatable("tooltip.copy.waypoint.coordinates")));
        edit_waypoint_button = ButtonWidget.builder(Text.translatable("selectWorld.edit"), button -> CListVariables.minecraft_client.setScreen(new CListWaypointConfig(Text.literal("Config"),selected_waypoint_id))).width(150).build();
        adderBottom.add(copy_coordinates_button,1, gridWidgetBottom.copyPositioner().marginBottom(10));
        adderBottom.add(edit_waypoint_button,1, gridWidgetBottom.copyPositioner().marginBottom(10));
        list = new ScrollList();
        list.SetupElements();
        addDrawableChild(list);
        gridWidget.refreshPositions();
        gridWidgetBottom.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, 0, 0, this.width, this.height, 0.5f, 0f);
        SimplePositioningWidget.setPos(gridWidgetBottom, 0, 0, this.width, this.height, 0.5f, 1f);
        gridWidget.forEachChild(this::addDrawableChild);
        gridWidgetBottom.forEachChild(this::addDrawableChild);
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        //list.render(context, mouseX, mouseY, delta);
        if(selected_waypoint_id >= 0){
            copy_coordinates_button.active = true;
            edit_waypoint_button.active = true;
        }
        else{
            copy_coordinates_button.active = false;
            edit_waypoint_button.active = false;
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        list.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        list.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        list.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    public class ScrollList extends EntryListWidget<ScrollList.ScrollListEntry> {
        public ScrollList(){
            super(CListWaypointScreen.this.client, CListWaypointScreen.this.width, CListWaypointScreen.this.height-64, 32, 25);//32
        }
        public void SetupElements(){
            for(int i = 0; i < CListClient.variables.waypoints.size(); i++){
                ScrollList.ScrollListEntry Coordinate = new ScrollList.ScrollListEntry(i);
                list.addEntry(Coordinate);
            }
        }
        public void RefreshElements(){
            clearEntries();
            SetupElements();
        }
        @Override
        public int getRowWidth() {
            return 245;
        }
        public void appendClickableNarrations(NarrationMessageBuilder builder){}
        public static class InvisibleButton extends ButtonWidget{
            public InvisibleButton(int x, int y, int width, int height, PressAction onPress){
                super(x, y, width, height, Text.literal(""), onPress,null);
            }
            @Override
            public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {}
        }
        public static class SpriteButton extends ButtonWidget {
            public int x_pos;
            public int y_pos;
            public int id;
            public SpriteButton(int x, int y, int width, int height, PressAction onPress, int coordinate_id) {
                super(x, y, width, height, Text.literal(""), onPress,null);
                this.id = coordinate_id;
                this.x_pos = x;
                this.y_pos = y;
            }
            @Override
            public void setX(int value){
                super.setX(value);
                this.x_pos = value;
            }
            @Override
            public void setY(int value){
                super.setY(value);
                this.y_pos = value;
            }
            @Override
            public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                Identifier eye_icon;
                if(CListClient.variables.waypoints.get(id).render){
                    eye_icon = new Identifier("coordinatelist", "icon/visible");
                }
                else{
                    eye_icon = new Identifier("coordinatelist", "icon/not_visible");
                }
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.drawGuiTexture(eye_icon, x_pos, y_pos, width, height);
                RenderSystem.disableBlend();
            }
        }
        public class ScrollListEntry extends EntryListWidget.Entry<ScrollListEntry>{
            public final Text waypoint_name;
            public final Text dimension;
            public final SpriteButton sh;
            public final InvisibleButton select;
            public final List<Element> children;
            public final int id;
            public ScrollListEntry(int id){
                this.id = id;
                this.waypoint_name = Text.of(CListClient.variables.waypoints.get(id).getName());
                this.dimension = CListClient.variables.waypoints.get(id).getDimensionText();
                this.sh = new SpriteButton(0, 0, 16, 12, button -> {
                    CListClient.variables.waypoints.get(id).toggleVisibility();
                    selected_waypoint_id = id;
                    CListWaypoint waypoint = CListClient.variables.waypoints.get(selected_waypoint_id);
                    copy_coordinates_button.setMessage(Text.literal(waypoint.x + " " + waypoint.y + " " + waypoint.z));
                }, id);
                this.select = new InvisibleButton(0, 0, 240, 25, button -> {
                    selected_waypoint_id = id;
                    CListWaypoint waypoint = CListClient.variables.waypoints.get(selected_waypoint_id);
                    copy_coordinates_button.setMessage(Text.literal(waypoint.x + " " + waypoint.y + " " + waypoint.z));
                });
                this.children = Lists.newArrayList();
                this.children.add(sh);
                this.children.add(select);
            }
            @Override
            public void render(DrawContext context, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
                sh.setX(x+2);
                sh.setY(y+4);
                select.setX(x);
                select.setY(y);
                sh.render(context, mouseX, mouseY, delta);
                select.render(context, mouseX, mouseY, delta);
                context.drawTextWithShadow(CListVariables.minecraft_client.textRenderer, dimension.getString(), x+180, y+6, 0xFFFFFF);
                context.drawTextWithShadow(CListVariables.minecraft_client.textRenderer, waypoint_name.getString(), x+22, y+6, CListClient.variables.colors.get(id).rgbToHex());
            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                boolean handled = false;
                for (Element E : children) {
                    if (E.mouseClicked(mouseX, mouseY, button)) {
                        handled = true;
                        break;
                    }
                }
                return handled || super.mouseClicked(mouseX, mouseY, button);
            }
            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                boolean handled = false;
                for (Element E : children) {
                    if (E.mouseReleased(mouseX, mouseY, button)) {
                        handled = true;
                        break;
                    }
                }
                return handled || super.mouseReleased(mouseX, mouseY, button);
            }
        }
    }
}