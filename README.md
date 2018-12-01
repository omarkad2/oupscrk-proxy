Oupscrk-proxy is a HTTP/HTTPS Proxy written in Java. You can intercept HTTP and HTTPS traffic, and display the intercepted stream *Deciphered* and *Decoded*.

## Website
Visit the website [Oupscrk](https://oupscrk.com) for a production-ready release.

## Features

- Expose all HTTP/HTTPS traffic in plain-text
- Tamper with HTTP/HTTPS Requests
- Tamper with HTTP/HTTPS Responses
- Perform Replay Attacks

## Dependencies
This program is written in Java 8 and uses Bouncy castle library as the Crypto API.

## Usage

As you may have noticed there is two profiles in the Pom file, which correspond to two usage modes :

1 - Standalone proxy (Default mode) : 
In this mode all you get is a proxy intercepting the HTTP/HTTPS traffic and the display of the streamed data between the navigator 
and the remote server in plain-text.

2 - Proxy UI : 
In this mode you have the prossibility to interact with the proxy using a TCP socket, and a set of actions. 
In the following an exhaustive list of actions : 
```
[START_PROXY, STOP_PROXY, CHECK_PROXY, START_EXPOSE, STOP_EXPOSE, START_TAMPER_REQUEST, 
STOP_TAMPER_REQUEST, START_TAMPER_RESPONSE, STOP_TAMPER_RESPONSE, START_REPLAY_ATTACK, 
STOP_REPLAY_ATTACK]
```

In the [Showcase website](https://oupscrk.com) I provide a desktop application that uses Proxy UI, for a better display and usability.

## Notice

In order to be able to intercept HTTPS traffic, you will have to load a custom CA (Certificate Authority) in your navigator.
To do so, you should first start the proxy and enable your navigator (firefox preferably) 
to use it :

![Alt text](docs/configure_proxy.png?raw=true "Proxy enabling on Firefox")

And then navigate to : http://oupscrk.local
![Alt text](docs/trust_CA.png?raw=true "Trust custom CA")

## Contribute
All new ideas/features are welcome.