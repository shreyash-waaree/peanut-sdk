package com.keenon.sdk.scmIot.protopack.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/Packet.class */
public class Packet implements Marshallable {
    protected Header header;
    protected List<Marshallable> contents;
    protected Unpack unpack;
    protected Pack pack;

    public Packet() {
        this.contents = new ArrayList();
        this.pack = new Pack();
    }

    public Packet(Header header) {
        this.contents = new ArrayList();
        this.pack = new Pack();
        this.header = header;
    }

    public Packet(Header header, Unpack unpack) {
        this.contents = new ArrayList();
        this.pack = new Pack();
        this.header = header;
        this.unpack = unpack;
    }

    public Packet(Header header, Marshallable... contents) {
        this(header, (List<Marshallable>) Arrays.asList(contents));
    }

    public Packet(Header header, List<Marshallable> contents) {
        this.contents = new ArrayList();
        this.pack = new Pack();
        this.header = header;
        this.contents = contents;
    }

    public Packet(Header header, Unpack unpack, Marshallable... contents) {
        this(header, unpack, (List<Marshallable>) Arrays.asList(contents));
    }

    public Packet(Header header, Unpack unpack, List<Marshallable> contents) {
        this.contents = new ArrayList();
        this.pack = new Pack();
        this.header = header;
        this.unpack = unpack;
        this.contents = contents;
    }

    public void marshalHeader(Pack pack) {
        if (this.header != null) {
            this.header.marshal(pack);
        }
    }

    public void unmarshalHeader(Unpack unpack) {
        if (this.header != null) {
            this.header.unmarshal(unpack);
        }
    }

    public void unmarshalContent(Unpack unpack) {
        if (this.contents != null && this.contents.size() != 0) {
            int length = this.contents.size();
            for (int i = 0; i < length; i++) {
                Marshallable mar = this.contents.get(i);
                unpack.popMarshallable(mar);
            }
        }
    }

    public void marshalContent(Pack pack) {
        if (this.contents != null && this.contents.size() != 0) {
            for (Marshallable mar : this.contents) {
                pack.putMarshallable(mar);
            }
        }
    }

    public void marshalHeader() {
        marshalHeader(this.pack);
    }

    public void unmarshalHeader() {
        unmarshalHeader(this.unpack);
    }

    public void unmarshalContent() {
        unmarshalContent(this.unpack);
    }

    public void marshalContent() {
        marshalContent(this.pack);
    }

    public Header getHeader() {
        return this.header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public List<Marshallable> getContents() {
        return this.contents;
    }

    public void setContents(List<Marshallable> contents) {
        this.contents = contents;
    }

    public void putContent(Marshallable content) {
        this.contents.add(content);
    }

    public void putContents(List<Marshallable> contents) {
        this.contents.addAll(contents);
    }

    public void popContent(Marshallable content, Unpack unpack) {
        unpack.popMarshallable(content);
        this.contents.add(content);
    }

    public void popContents(List<Marshallable> contents, Unpack unpack) {
        if (contents != null && contents.size() != 0) {
            for (Marshallable mar : contents) {
                unpack.popMarshallable(mar);
                this.contents.add(mar);
            }
        }
    }

    public void marshal() {
        marshal(this.pack);
    }

    public void unmarshal() {
        unmarshal(this.unpack);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        marshalHeader(pack);
        marshalContent(pack);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        unmarshalHeader(unpack);
        unmarshalContent(unpack);
    }

    public Pack getPack() {
        return this.pack;
    }

    public String toString() {
        String str = super.toString() + " packet header:" + this.header.toString() + " contents:" + this.contents.toString();
        return str;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return null;
    }

    public byte[] getBytes() {
        return this.pack.getBytes();
    }
}
