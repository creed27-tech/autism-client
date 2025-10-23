/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import org.joml.Vector2d;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL32C.*;

public class Mesh {
    private final VertexBuffer vb;
    private final IndexBuffer ib;

    private final DrawMode drawMode;
    private final Attrib[] attribs;
    private final int stride;

    private final List<Integer> indices = new ArrayList<>();
    private int verticesCount;

    private ByteBuffer buffer;

    protected Mesh(DrawMode drawMode, Attrib... attributes) {
        this.vb = new VertexBuffer();
        this.ib = new IndexBuffer();

        this.drawMode = drawMode;
        this.attribs = attributes;

        int s = 0;
        for (Attrib attrib : attribs) s += attrib.size;
        this.stride = s;
    }

    public void begin() {
        if (buffer == null) buffer = ByteBuffer.allocateDirect(32 * stride);

        indices.clear();
        verticesCount = 0;
    }

    public void line(int i1, int i2) {
        indices.add(i1);
        indices.add(i2);
    }

    public void tri(int i1, int i2, int i3) {
        indices.add(i1);
        indices.add(i2);
        indices.add(i3);
    }

    public void quad(int i1, int i2, int i3, int i4) {
        indices.add(i1);
        indices.add(i2);
        indices.add(i3);

        indices.add(i3);
        indices.add(i4);
        indices.add(i1);
    }

    public void triangleFan(int i1, int length) {
        for (int i = 0; i < length - 2; i++) {
            indices.add(i1);
            indices.add(i1 + i + 1);
            indices.add(i1 + i + 2);
        }
    }

    protected void growIfNeeded() {
        // Grow buffer
        while (buffer.position() + stride > buffer.capacity()) {
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2);

            buffer.flip();
            newBuffer.put(buffer);

            buffer = newBuffer;
        }
    }

    public Mesh vec2(double x, double y) {
        growIfNeeded();

        buffer.putFloat((float) x);
        buffer.putFloat((float) y);

        return this;
    }

    public Mesh vec3(double x, double y, double z) {
        growIfNeeded();

        buffer.putFloat((float) x);
        buffer.putFloat((float) y);
        buffer.putFloat((float) z);

        return this;
    }

    public Mesh color(Color color) {
        growIfNeeded();

        buffer.put((byte) color.r);
        buffer.put((byte) color.g);
        buffer.put((byte) color.b);
        buffer.put((byte) color.a);

        return this;
    }

    public int next() {
        return verticesCount++;
    }

    public void end() {
        if (buffer == null) return;

        // Upload vertices
        buffer.flip();
        vb.upload(buffer);
        buffer.clear();

        // Upload indices
        ib.upload(indices);
    }

    public void render() {
        if (ib.count <= 0) return;

        vb.bind();
        ib.bind();

        bindAttribs();
        glDrawElements(drawMode.getGL(), ib.count, GL_UNSIGNED_INT, 0);
        unbindAttribs();

        ib.unbind();
        vb.unbind();
    }

    private void bindAttribs() {
        int offset = 0;

        for (int i = 0; i < attribs.length; i++) {
            Attrib attrib = attribs[i];

            glEnableVertexAttribArray(i);

            switch (attrib.type) {
                case Float:  glVertexAttribPointer(i, attrib.count, GL_FLOAT, false, stride, offset); break;
                case UByte:  glVertexAttribPointer(i, attrib.count, GL_UNSIGNED_BYTE, true, stride, offset); break;
            }

            offset += attrib.size;
        }
    }

    private void unbindAttribs() {
        for (int i = 0; i < attribs.length; i++) {
            glDisableVertexAttribArray(i);
        }
    }

    // VertexBuffer

    private static class VertexBuffer {
        private int id;

        public VertexBuffer() {
            this.id = glGenBuffers();
        }

        public void bind() {
            glBindBuffer(GL_ARRAY_BUFFER, id);
        }

        public void unbind() {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        public void upload(ByteBuffer buffer) {
            bind();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
            unbind();
        }
    }

    // IndexBuffer

    private static class IndexBuffer {
        private int id;
        private int count;

        public IndexBuffer() {
            this.id = glGenBuffers();
        }

        public void bind() {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
        }

        public void unbind() {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        }

        public void upload(List<Integer> indices) {
            count = indices.size();

            bind();
            GL.INSTANCE.bufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
            unbind();
        }
    }

    // Attrib

    public enum Attrib {
        Vec2(Type.Float, 2),
        Vec3(Type.Float, 3),
        Color(Type.UByte, 4);

        private final Type type;
        private final int count;
        private final int size;

        Attrib(Type type, int count) {
            this.type = type;
            this.count = count;
            this.size = type.size * count;
        }
    }

    private enum Type {
        Float(4),
        UByte(1);

        private final int size;

        Type(int size) {
            this.size = size;
        }
    }
}
