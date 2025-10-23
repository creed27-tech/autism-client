/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Renderer2D {
    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;

    private final Mesh mesh;
    private final boolean textured;

    private boolean building;

    public Renderer2D(boolean textured) {
        this.mesh = new ShaderMesh(Shaders.POS_TEX_COLOR, DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
        this.textured = textured;
    }

    @PreInit
    public static void init() {
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
    }

    public void begin() {
        if (building) throw new IllegalStateException("Renderer2D.begin() called twice");

        building = true;
        mesh.begin();
    }

    public void end() {
        if (!building) throw new IllegalStateException("Renderer2D.end() called without calling Renderer2D.begin()");

        mesh.end();
        building = false;
    }

    public void render(TextureRegion texture) {
        if (!textured) throw new IllegalStateException("Rendering with a texture while using non textured renderer");

        Texture.MISSING.bindTexture();
        if (texture != null) texture.bind();

        mesh.render();
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft, TextureRegion texture) {
        quad(
                x, y,
                x + width, y,
                x + width, y + height,
                x, y + height,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft,
                texture
        );
    }

    public void quad(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft, TextureRegion texture) {
        double minU = textured ? texture.u1 : 0;
        double minV = textured ? texture.v1 : 0;
        double maxU = textured ? texture.u2 : 0;
        double maxV = textured ? texture.v2 : 0;

        mesh.quad(
            mesh.vec2(x1, y1).vec2(minU, minV).color(cTopLeft).next(),
            mesh.vec2(x2, y2).vec2(maxU, minV).color(cTopRight).next(),
            mesh.vec2(x3, y3).vec2(maxU, maxV).color(cBottomRight).next(),
            mesh.vec2(x4, y4).vec2(minU, maxV).color(cBottomLeft).next()
        );
    }

    public void texture(double x, double y, double width, double height, TextureRegion texture) {
        quad(x, y, width, height, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, texture);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color, color, color, null);
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft, null);
    }

    public void quad(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, Color color) {
        quad(x1, y1, x2, y2, x3, y3, x4, y4, color, color, color, color, null);
    }

    public void tri(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        tri(x1, y1, x2, y2, x3, y3, color, color, color, null);
    }

    public void tri(double x1, double y1, double x2, double y2, double x3, double y3, Color c1, Color c2, Color c3, TextureRegion texture) {
        double minU = textured ? texture.u1 : 0;
        double minV = textured ? texture.v1 : 0;
        double maxU = textured ? texture.u2 : 0;
        double maxV = textured ? texture.v2 : 0;

        mesh.tri(
            mesh.vec2(x1, y1).vec2(minU, minV).color(c1).next(),
            mesh.vec2(x2, y2).vec2(maxU, minV).color(c2).next(),
            mesh.vec2(x3, y3).vec2(maxU, maxV).color(c3).next()
        );
    }

    public void circleOutline(double x, double y, double r, int segments, double width, Color color) {
        double circumference = 2 * Math.PI * r;
        double step = circumference / segments;

        double angle = 0;
        double lastY = y + r;
        double lastX = x;

        for (int i = 0; i < segments; i++) {
            angle += step / r;

            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            double newX = x + sin * r;
            double newY = y + cos * r;

            lineWidth(lastX, lastY, newX, newY, width, color);

            lastX = newX;
            lastY = newY;
        }
    }

    public void circle(double x, double y, double r, Color color) {
        circle(x, y, r, 50, color);
    }

    public void circle(double x, double y, double r, int segments, Color color) {
        mesh.vec2(x, y).vec2(0, 0).color(color).next();

        double step = 2 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = i * step;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            mesh.vec2(x + sin * r, y + cos * r).vec2(0, 0).color(color).next();
        }

        mesh.triangleFan(0, segments + 1);
    }

    public void lineWidth(double x1, double y1, double x2, double y2, double width, Color color) {
        double angle = Math.atan2(y2 - y1, x2 - x1) + Math.PI / 2;
        double sin = Math.sin(angle) * width / 2;
        double cos = Math.cos(angle) * width / 2;

        quad(x1 + cos, y1 + sin, x2 + cos, y2 + sin, x2 - cos, y2 - sin, x1 - cos, y1 - sin, color);
    }
}
