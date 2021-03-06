# request-logging project
Spring boot starter for request and response logging.

It adds filter with the highest precedence.

## How to use
```
dependencies {
    implementation("com.etherealscope:request-logging-spring-boot-starter:5.0.0")
}
```

## Config keys with examples
```
ethereal.logging.enabled=true
ethereal.logging.include-time-elapsed=true
ethereal.logging.status-codes=sc_any

ethereal.logging.request.enabled=true
ethereal.logging.request.include-headers=true
ethereal.logging.request.include-payload=true
ethereal.logging.request.include-query-params=true
ethereal.logging.request.include-ip-address=false
ethereal.logging.request.max-payload-size=4096
ethereal.logging.request.white-listed-content-types=application/json
ethereal.logging.request.black-listed-content-types=image/png
ethereal.logging.request.white-listed-servlet-paths=/public/**
ethereal.logging.request.black-listed-servlet-paths=/public/**
ethereal.logging.request.masks[0].path-matcher=/**
ethereal.logging.request.masks[0].masked-headers=authorization,cookie
ethereal.logging.request.masks[1].method=POST
ethereal.logging.request.masks[1].path-matcher=/login
ethereal.logging.request.masks[1].masked-query-params=password

ethereal.logging.response.enabled=true
ethereal.logging.response.include-headers=true
ethereal.logging.response.include-payload=true
ethereal.logging.response.max-payload-size=4096
ethereal.logging.response.white-listed-content-types=application/json
ethereal.logging.response.black-listed-content-types=image/png
ethereal.logging.response.white-listed-servlet-paths=/public/**
ethereal.logging.response.black-listed-servlet-paths=/public/**
ethereal.logging.response.masks[0].path-matcher=/**
ethereal.logging.response.masks[0].masked-json-fields=password,oldPassword
```