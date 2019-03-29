package com.ljz.agent.registry;

public class Endpoint {
    private final String host;
    private final int port;
    private final int loadLevel;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLoadLevel() {
        return loadLevel;
    }

    public String getName(){
        return host + ":" + String.valueOf(port);
    }

    public Endpoint(String host, int port, int loadLevel){
        this.host = host;
        this.port = port;
        this.loadLevel = loadLevel;
    }


    public boolean equals(Object o){
        if (!(o instanceof Endpoint)){
            return false;
        }
        Endpoint other = (Endpoint) o;
        return other.host.equals(this.host) && other.port == this.port;
    }

    public int hashCode(){
        return host.hashCode() + port;
    }
}
