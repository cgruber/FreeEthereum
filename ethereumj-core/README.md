
## free-ethereum-core

### Build from source

#### Compile, test and package

Run `../gradlew build`.

 - find jar artifacts at `build/libs`
 - find unit test and code coverage reports at `build/reports`

#### Run an ethereum node

 - run `../gradlew run`, or
 - build a standalone executable jar with `../gradlew shadow` and execute the `-all` jar in `build/libs` using `java -jar [jarfile]`.
