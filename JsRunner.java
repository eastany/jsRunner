import javax.script.ScriptEngine;
import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.net.*;

import com.sun.net.httpserver.*;
/**
 * Created by eastany on 15/4/5.
 */

public class JsRunner {

    public static String getRsaJS() {
        StringBuffer  buffer = new StringBuffer();
        try {
            BufferedReader br=new BufferedReader(new FileReader("/xxxxx/js_runner/src/core.js"));
            String line= "";
            while((line=br.readLine())!=null){
                buffer.append(line);
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        System.out.println(buffer.toString());
        return buffer.toString();
    }

    public static void main(String[] args) throws Exception {
        JsRunner server = new JsRunner("/var/www", 8080);
        server.service();
    }

    public static String WEB_ROOT = "/var/www";
    //端口
    private int port;
    private String requestPath;
    public JsRunner(String root, int port) {
        WEB_ROOT = root;
        this.port = port;
        requestPath = null;
    }
    //处理GET请求
    static class GetHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String uri = t.getRequestURI().getRawQuery();
            String sts = "";
            String passwd = "";
            String key = "";
            if(uri.length()>0){
                String[] ps = uri.split("&");
                for(int i=0;i<ps.length;i++){
                    String[] kv = ps[i].split("=");
                    if(kv.length>1){
                        if(kv[0].equals("sts")){
                            sts = kv[1];
                        }else if(kv[0].equals("passwd")){
                            passwd = kv[1];
                        }else if(kv[0].equals("key")){
                            key = kv[1];
                        }
                    }
                }
                if(passwd!=""&&key!=""&&sts!="") {
                    String pass = "";
                    try {
                        ScriptEngineManager sem = new ScriptEngineManager();
                        ScriptEngine se = sem.getEngineByName("javascript");
                        String js = getRsaJS();
                        se.eval(js);
                        if (se instanceof Invocable) {
                            Invocable invoke = (Invocable) se;
                            pass += invoke.invokeFunction("my_encrypt", passwd, sts).toString();

                            System.out.println(invoke.invokeFunction("test").toString());
                        }

                    } catch (Exception e){
                        System.out.println(e.getMessage());
                        pass = e.getMessage();
                    } finally {
                        Headers h = t.getResponseHeaders();
                        h.add("Content-Type", "application/json");
                        t.sendResponseHeaders(200, pass.length());
                        OutputStream os = t.getResponseBody();
                        os.write(pass.getBytes(),0,pass.getBytes().length);
                        os.close();
                    }

                }
            }

        }
    }

    public void service() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/get", new GetHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
