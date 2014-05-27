import org.ocamljava.runtime.values.BlockValue;
import org.ocamljava.runtime.values.Value;
import org.ocamljava.runtime.kernel.AbstractNativeRunner;
import org.ocamljava.runtime.kernel.Fail;
import org.ocamljava.runtime.kernel.Fatal;
import org.ocamljava.runtime.kernel.NativeApply;
import org.ocamljava.runtime.wrappers.*;

public final class OcamlWrapper {

    /**
     * No instance of this class.
     */
    private OcamlWrapper() {
    }


    public static final class string_int_tuple extends OCamlValue {

        public static final Wrapper<string_int_tuple> WRAPPER = new SimpleWrapper<string_int_tuple>() { public string_int_tuple wrap(final Value v) { return new string_int_tuple(v); } };

        private string_int_tuple(final Value v) {
            super(v);
        }

        public Wrapper<? extends string_int_tuple> getWrapper(final int idx) {
            return string_int_tuple.WRAPPER;
        }

        public Wrapper<? extends string_int_tuple> getWrapper() {
            return string_int_tuple.WRAPPER;
        }

        public String getString_val() {
            return this.value.get0().asString();
        }

        public long getInt_Val() {
            return this.value.get1().asLong();
        }

        @Override
        public int hashCode() {
            return this.value.hashCode();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof string_int_tuple) {
                final string_int_tuple that = (string_int_tuple) obj;
                return (this.value.get0().asString().equals(that.value.get0().asString())) && ((this.value.get1().asLong()) == (that.value.get1().asLong()));
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("string_int_tuple(");
            sb.append("string_val=");
            sb.append(this.value.get0().asString());
            sb.append(", int_Val=");
            sb.append(this.value.get1().asLong());
            sb.append(")");
            return sb.toString();
        }

        public static string_int_tuple create(final String v0, final long v1) {
            return new string_int_tuple(Value.createBlock(0, Value.createString(v0), Value.createLong(v1)));
        }

        public static string_int_tuple wrap(final Value v) {
            return new string_int_tuple(v);
        }

        public static Wrapper<? extends string_int_tuple> wrapper() {
            return string_int_tuple.WRAPPER;
        }

    }

}

