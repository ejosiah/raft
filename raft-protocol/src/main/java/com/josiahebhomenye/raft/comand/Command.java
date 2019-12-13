package com.josiahebhomenye.raft.comand;

import com.josiahebhomenye.raft.Divide;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.*;
import java.util.Objects;

@RequiredArgsConstructor
public abstract class Command {

    @Getter
    protected final int value;

    public static final int SET = 1;
    public static final int ADD = 1 << 1;
    public static final int SUBTRACT = 1 << 2;
    public static final int MULTIPLY = 1 << 3;
    public static final int DIVIDE = 1 << 4;

    public abstract void apply(Data data);

    public abstract int id();

    @SneakyThrows
    public byte[] serialize() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutput in = new DataOutputStream(buf);
        in.writeInt(id());
        in.writeInt(value);
        return buf.toByteArray();
    }

    @SneakyThrows
    public static Command restore(byte[] data){
        DataInput in = new DataInputStream(new ByteArrayInputStream(data));
        int commandType = in.readInt();
        switch (commandType){
            case SET: return new Set(in.readInt());
            case ADD: return new Add(in.readInt());
            case SUBTRACT: return new Subtract(in.readInt());
            case MULTIPLY: return new Multiply(in.readInt());
            case DIVIDE: return new Divide(in.readInt());
            default: throw new IllegalArgumentException("unable to create command from bytes");
        }
    }

    public String name(){
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), value);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command)) return false;
        Command command = (Command) o;
        return value == command.value && this.id() == ((Command) o).id();
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, id());
    }
}
