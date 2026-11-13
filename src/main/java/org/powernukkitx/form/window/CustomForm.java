package org.powernukkitx.form.window;

import lombok.extern.slf4j.Slf4j;
import org.powernukkitx.Player;
import org.powernukkitx.form.element.ElementDivider;
import org.powernukkitx.form.element.ElementHeader;
import org.powernukkitx.form.element.ElementLabel;
import org.powernukkitx.form.element.custom.*;
import org.powernukkitx.form.response.CustomResponse;
import org.powernukkitx.form.response.ElementResponse;
import org.powernukkitx.utils.JSONUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cloudburstmc.protocol.bedrock.data.ModalFormCancelReason;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
@NoArgsConstructor
@Slf4j
public class CustomForm extends Form<CustomResponse> {
    protected static Type LIST_STRING_TYPE = new TypeToken<List<String>>(){}.getType();

    protected ObjectArrayList<ElementCustom> elements = new ObjectArrayList<>();

    protected String submitButton;

    public CustomForm(String title) {
        super(title);
    }

    public CustomForm addElement(ElementCustom element) {
        this.elements.add(element);
        return this;
    }

    public CustomForm addElements(ElementCustom... elements) {
        for (ElementCustom element : elements) {
            this.addElement(element);
        }

        return this;
    }

    public CustomForm addLabel(String text) { return this.addElement(new ElementLabel(text)); }

    public CustomForm addInput(String text) {
        return this.addElement(new ElementInput(text));
    }

    public CustomForm addInput(String text, String placeholder) {
        return this.addElement(new ElementInput(text, placeholder));
    }

    public CustomForm addInput(String text, String placeholder, String defaultText) {
        return this.addElement(new ElementInput(text, placeholder, defaultText));
    }

    public CustomForm addInput(String text, String placeholder, String defaultText, String tooltip) {
        return this.addElement(new ElementInput(text, placeholder, defaultText, tooltip));
    }

    public CustomForm addDropdown(String text) {
        return this.addElement(new ElementDropdown(text));
    }

    public CustomForm addDropdown(String text, String tooltip) {
        return this.addElement(new ElementDropdown(text, tooltip));
    }

    public CustomForm addDropdown(String text, List<String> options) {
        return this.addElement(new ElementDropdown(text, options));
    }

    public CustomForm addDropdown(String text, List<String> options, int defaultOption) {
        return this.addElement(new ElementDropdown(text, options, defaultOption));
    }

    public CustomForm addDropdown(String text, List<String> options, String tooltip) {
        return this.addElement(new ElementDropdown(text, options, tooltip));
    }

    public CustomForm addSlider(String text) {
        return this.addElement(new ElementSlider(text));
    }

    public CustomForm addSlider(String text, float min, float max) {
        return this.addElement(new ElementSlider(text, min, max));
    }

    public CustomForm addSlider(String text, float min, float max, int step) {
        return this.addElement(new ElementSlider(text, min, max, step));
    }

    public CustomForm addSlider(String text, float min, float max, int step, float defaultValue) {
        return this.addElement(new ElementSlider(text, min, max, step, defaultValue));
    }

    public CustomForm addSlider(String text, float min, float max, int step, float defaultValue, String tooltip) {
        return this.addElement(new ElementSlider(text, min, max, step, defaultValue, tooltip));
    }

    public CustomForm addStepSlider(String text) {
        return this.addElement(new ElementStepSlider(text));
    }

    public CustomForm addStepSlider(String text, List<String> steps) {
        return this.addElement(new ElementStepSlider(text, steps));
    }

    public CustomForm addStepSlider(String text, List<String> steps, int defaultStep) {
        return this.addElement(new ElementStepSlider(text, steps, defaultStep));
    }

    public CustomForm addStepSlider(String text, List<String> steps, int defaultStep, String tooltip) {
        return this.addElement(new ElementStepSlider(text, steps, defaultStep, tooltip));
    }

    public CustomForm addToggle(String text) {
        return this.addElement(new ElementToggle(text));
    }

    public CustomForm addToggle(String text, boolean defaultValue) {
        return this.addElement(new ElementToggle(text, defaultValue));
    }

    public CustomForm addToggle(String text, boolean defaultValue, String tooltip) {
        return this.addElement(new ElementToggle(text, defaultValue, tooltip));
    }

    public CustomForm submitButton() {
        this.submitButton = null;
        return this;
    }

    @Override
    public CustomForm onSubmit(BiConsumer<Player, CustomResponse> submitted) {
        return (CustomForm) super.onSubmit(submitted);
    }

    @Override
    public CustomForm onClose(Consumer<Player> callback) {
        return (CustomForm) super.onClose(callback);
    }

    @Override
    public CustomForm onCancel(BiConsumer<Player, ModalFormCancelReason> cancel) {
        return (CustomForm) super.onCancel(cancel);
    }

    @Override
    public CustomForm send(Player player) {
        return (CustomForm) super.send(player);
    }

    @Override
    public CustomForm send(Player player, int id) {
        return (CustomForm) super.send(player, id);
    }

    @Override
    public CustomForm sendUpdate(Player player) {
        return (CustomForm) super.sendUpdate(player);
    }

    @Override
    public String windowType() {
        return "custom_form";
    }

    @Override
    public String toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.windowType());
        object.addProperty("title", this.title);

        JsonArray elementArray = new JsonArray();
        this.elements().forEach(element -> elementArray.add(element.toJson()));

        object.add("content", elementArray);

        if (this.submitButton != null && !this.submitButton.isEmpty()) {
            object.addProperty("submit", this.submitButton);
        }

        return object.toString();
    }

    @Override
    public CustomResponse respond(Player player, String formData, ModalFormCancelReason cancelReason) {
        if(cancelReason != null) {
            if(cancelReason == ModalFormCancelReason.USER_CLOSED) this.supplyClosed(player);
            else this.supplyCancelled(player, cancelReason);
            return null;
        }

        if (!super.handle(player, formData)) {
            this.supplyClosed(player);
            return null;
        }

        CustomResponse response = new CustomResponse();

        final List<String> elementResponses;
        try {
            elementResponses = JSONUtils.from(formData, LIST_STRING_TYPE);
        } catch (RuntimeException e) {
            log.debug("Discarding malformed custom form response from {}", player.getName());
            this.supplyClosed(player);
            return null;
        }

        if (elementResponses == null) {
            this.supplyClosed(player);
            return null;
        }

        for (int i = 0, responseSize = elementResponses.size(); i < responseSize; i++) {
            if (i >= this.elements.size()) {
                break;
            }

            String responseData = elementResponses.get(i);
            ElementCustom element = this.elements.get(i);

            Object elementResponse = null;

            switch (element) {
                case ElementDropdown dropdown -> {
                    Integer index = parseIndex(responseData, dropdown.options().size());
                    if (index == null) {
                        return this.rejectResponse(player, i);
                    }
                    elementResponse = new ElementResponse(index, dropdown.options().get(index));
                }
                case ElementInput input -> elementResponse = responseData == null ? "" : responseData;
                case ElementSlider slider -> {
                    Float value = parseSliderValue(responseData, slider);
                    if (value == null) {
                        return this.rejectResponse(player, i);
                    }
                    elementResponse = value;
                }
                case ElementStepSlider stepSlider -> {
                    Integer index = parseIndex(responseData, stepSlider.steps().size());
                    if (index == null) {
                        return this.rejectResponse(player, i);
                    }
                    elementResponse = new ElementResponse(index, stepSlider.steps().get(index));
                }
                case ElementToggle toggle -> elementResponse = Boolean.parseBoolean(responseData);
                case ElementDivider divider -> elementResponse = divider.text();
                case ElementHeader header -> elementResponse = header.text();
                case ElementLabel label -> elementResponse = label.text();
                default -> {}
            }

            response.setResponse(i, elementResponse);
        }

        this.supplySubmitted(player, response);
        return response;
    }

    private CustomResponse rejectResponse(Player player, int index) {
        log.debug("Discarding custom form response from {}: element {} was not offered", player.getName(), index);
        this.supplyClosed(player);
        return null;
    }

    private static Integer parseIndex(String responseData, int size) {
        if (responseData == null) {
            return null;
        }
        final int index;
        try {
            index = Integer.parseInt(responseData.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        return index >= 0 && index < size ? index : null;
    }

    private static Float parseSliderValue(String responseData, ElementSlider slider) {
        if (responseData == null) {
            return null;
        }
        final float value;
        try {
            value = Float.parseFloat(responseData.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (!Float.isFinite(value)) {
            return null;
        }
        final float low = Math.min(slider.min(), slider.max());
        final float high = Math.max(slider.min(), slider.max());
        return Math.clamp(value, low, high);
    }

    @Override
    public <M> CustomForm putMeta(String key, M object) {
        return (CustomForm) super.putMeta(key, object);
    }
}
