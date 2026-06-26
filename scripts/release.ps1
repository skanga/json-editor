$ErrorActionPreference = "Stop"

$version = (& mvn -q -DforceStdout help:evaluate -Dexpression=project.version).Trim()
if ([string]::IsNullOrWhiteSpace($version)) {
    throw "Could not read Maven project.version"
}

$tag = "v$version"
$localJar = "target/json-editor.jar"
$asset = "target/json-editor-$version.jar"

mvn -q clean package
if (!(Test-Path $localJar)) {
    throw "Expected local JAR not found: $localJar"
}
Copy-Item -LiteralPath $localJar -Destination $asset -Force

git diff --quiet
if ($LASTEXITCODE -ne 0) {
    throw "Working tree has uncommitted changes"
}

git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    throw "Index has uncommitted changes"
}

git tag $tag
git push origin $tag
gh release create $tag $asset --title $tag --notes-file CHANGELOG.md
