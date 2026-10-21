package cn.quiet.tv;

import android.os.IBinder;
import android.os.Parcel;

/** Read-only adapter to the installed Whaley firmware's input and HDMI-CEC services. */
final class WhaleyInputs {
    static final String SERVICE="tv.whaley.api.aidl.IWtvApiServiceAidl";
    static final String CEC="tv.whaley.api.aidl.IWtvApiCecAidl";
    static final String SOURCE="tv.whaley.api.aidl.IWtvApiInputSourceAidl";
    static final class Snapshot {
        final String[] names=new String[3];
        boolean[] connected;
        int previous=-1;
        String error;
        String hdmiLabel(int index){
            String port="HDMI "+(index+1);
            if(connected!=null&&connected.length>index+4&&!connected[index+4])return port+" — 未连接";
            String name=names[index];
            if(name!=null){name=name.trim();if(name.isEmpty()||name.equals("HDMI设备")||name.equalsIgnoreCase("HDMI Device"))name=null;}
            String detail=name!=null?name:connected!=null?"已连接，未识别名称":"状态未读取";
            return port+" — "+detail;
        }
    }
    static Snapshot read(){
        Snapshot result=new Snapshot();
        try{
            IBinder service=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"WTV_API_SERVICE");
            if(service==null)throw new IllegalStateException("WTV service unavailable");
            IBinder cec=child(service,11),source=child(service,4);
            if(source!=null){
                Parcel d=Parcel.obtain(),r=Parcel.obtain();
                try{d.writeInterfaceToken(SOURCE);source.transact(3,d,r,0);r.readException();result.connected=r.createBooleanArray();}finally{d.recycle();r.recycle();}
                d=Parcel.obtain();r=Parcel.obtain();
                try{d.writeInterfaceToken(SOURCE);source.transact(24,d,r,0);r.readException();if(r.readInt()!=0)result.previous=r.readInt();}finally{d.recycle();r.recycle();}
            }
            if(cec!=null)for(int index=0;index<3;index++){
                Parcel d=Parcel.obtain(),r=Parcel.obtain();
                try{d.writeInterfaceToken(CEC);d.writeInt(index);d.writeInt(0);cec.transact(6,d,r,0);r.readException();result.names[index]=r.readString();}finally{d.recycle();r.recycle();}
            }
        }catch(Exception e){result.error=e.toString();}
        return result;
    }
    static IBinder child(IBinder service,int transaction)throws Exception{
        Parcel d=Parcel.obtain(),r=Parcel.obtain();
        try{d.writeInterfaceToken(SERVICE);service.transact(transaction,d,r,0);r.readException();return r.readStrongBinder();}finally{d.recycle();r.recycle();}
    }
}
