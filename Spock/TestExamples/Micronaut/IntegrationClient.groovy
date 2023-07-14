package com.ssi.integration;

import com.ssi.integration.*;
import io.micronaut.core.annotation.*;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.*;

import javax.validation.*;
import javax.validation.constraints.*;
import java.util.*;

@Client("/ssi")
interface IntegrationClient {
    @Get
    Iterable<Integration> list();

    @Post
    HttpResponse<Integration> create(@NonNull @NotNull @Valid @Body IntegrationCommand integration);

    @Put
    Optional<Integration> update(@NonNull @NotNull @Valid @Body IntegrationCommand integration);

    @Get("/{companyCode}")
    Optional<Integration> find(@NonNull @NotBlank @PathVariable String companyCode);

    @NonNull
    @Delete
    HttpStatus delete(@NonNull @Valid @Body IntegrationCommand integration);
}