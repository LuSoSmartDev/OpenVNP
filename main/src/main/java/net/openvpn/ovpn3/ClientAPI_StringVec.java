package net.openvpn.ovpn3;

public class ClientAPI_StringVec {
    protected transient boolean swigCMemOwn;
    private transient long swigCPtr;

    protected ClientAPI_StringVec(long j, boolean z) {
        this.swigCMemOwn = z;
        this.swigCPtr = j;
    }

    protected static long getCPtr(ClientAPI_StringVec clientAPI_StringVec) {
        if (clientAPI_StringVec == null) {
            return 0;
        }
        return clientAPI_StringVec.swigCPtr;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        delete();
    }

    public synchronized void delete() {
        if (this.swigCPtr != 0) {
            if (this.swigCMemOwn) {
                this.swigCMemOwn = false;
                ovpncliJNI.delete_ClientAPI_StringVec(this.swigCPtr);
            }
            this.swigCPtr = 0;
        }
    }

    public ClientAPI_StringVec() {
        this(ovpncliJNI.new_ClientAPI_StringVec__SWIG_0(), true);
    }

    public ClientAPI_StringVec(long j) {
        this(ovpncliJNI.new_ClientAPI_StringVec__SWIG_1(j), true);
    }

    public long size() {
        return ovpncliJNI.ClientAPI_StringVec_size(this.swigCPtr, this);
    }

    public long capacity() {
        return ovpncliJNI.ClientAPI_StringVec_capacity(this.swigCPtr, this);
    }

    public void reserve(long j) {
        ovpncliJNI.ClientAPI_StringVec_reserve(this.swigCPtr, this, j);
    }

    public boolean isEmpty() {
        return ovpncliJNI.ClientAPI_StringVec_isEmpty(this.swigCPtr, this);
    }

    public void clear() {
        ovpncliJNI.ClientAPI_StringVec_clear(this.swigCPtr, this);
    }

    public void add(String str) {
        ovpncliJNI.ClientAPI_StringVec_add(this.swigCPtr, this, str);
    }

    public String get(int i) {
        return ovpncliJNI.ClientAPI_StringVec_get(this.swigCPtr, this, i);
    }

    public void set(int i, String str) {
        ovpncliJNI.ClientAPI_StringVec_set(this.swigCPtr, this, i, str);
    }
}
