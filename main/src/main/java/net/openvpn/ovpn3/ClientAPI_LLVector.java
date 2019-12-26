package net.openvpn.ovpn3;

public class ClientAPI_LLVector {
    protected transient boolean swigCMemOwn;
    private transient long swigCPtr;

    protected ClientAPI_LLVector(long j, boolean z) {
        this.swigCMemOwn = z;
        this.swigCPtr = j;
    }

    protected static long getCPtr(ClientAPI_LLVector clientAPI_LLVector) {
        if (clientAPI_LLVector == null) {
            return 0;
        }
        return clientAPI_LLVector.swigCPtr;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        delete();
    }

    public synchronized void delete() {
        if (this.swigCPtr != 0) {
            if (this.swigCMemOwn) {
                this.swigCMemOwn = false;
                ovpncliJNI.delete_ClientAPI_LLVector(this.swigCPtr);
            }
            this.swigCPtr = 0;
        }
    }

    public ClientAPI_LLVector() {
        this(ovpncliJNI.new_ClientAPI_LLVector__SWIG_0(), true);
    }

    public ClientAPI_LLVector(long j) {
        this(ovpncliJNI.new_ClientAPI_LLVector__SWIG_1(j), true);
    }

    public long size() {
        return ovpncliJNI.ClientAPI_LLVector_size(this.swigCPtr, this);
    }

    public long capacity() {
        return ovpncliJNI.ClientAPI_LLVector_capacity(this.swigCPtr, this);
    }

    public void reserve(long j) {
        ovpncliJNI.ClientAPI_LLVector_reserve(this.swigCPtr, this, j);
    }

    public boolean isEmpty() {
        return ovpncliJNI.ClientAPI_LLVector_isEmpty(this.swigCPtr, this);
    }

    public void clear() {
        ovpncliJNI.ClientAPI_LLVector_clear(this.swigCPtr, this);
    }

    public void add(long j) {
        ovpncliJNI.ClientAPI_LLVector_add(this.swigCPtr, this, j);
    }

    public long get(int i) {
        return ovpncliJNI.ClientAPI_LLVector_get(this.swigCPtr, this, i);
    }

    public void set(int i, long j) {
        ovpncliJNI.ClientAPI_LLVector_set(this.swigCPtr, this, i, j);
    }
}
