#!/usr/bin/env python3
"""Version-pinned Material Rounded icon extraction and exact candidate preflight.

Runs in GitHub Actions from driver branch. Never pushes main or publishes releases.
The candidate is pushed only after APK compilation and byte-for-byte product asset audit.
"""
from pathlib import Path
import hashlib
import os
import re
import shutil
import subprocess
import urllib.request
import zipfile

REPO = Path.cwd()
SPEC = "e623ba867e6e56c7bc9c6ac7b69cb28ac03e6aea"
BASE = "3492add5cb3f74b5ffe4e477055be7aa676fb6c4"
BRANCH = "candidate/apk-size-icons-v1-20261017"
VERSION = "1.7.8"
STABLE_SIZE = 387885070
STABLE_HASH = "c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e"
GEO_SHA = "7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb"
SRC = REPO / "app/src/main/java"
RUNNER_TMP = Path("/tmp/arihna-icons-preflight")
RUNNER_TMP.mkdir(exist_ok=True)

def shell(*args, **kw):
    print("+", " ".join(map(str,args)), flush=True)
    return subprocess.check_output(args, text=True, **kw).strip()

def sha256(path):
    h=hashlib.sha256()
    with open(path,"rb") as f:
        for chunk in iter(lambda:f.read(2**20),b""):
            h.update(chunk)
    return h.hexdigest()

def fetch_artifact(name):
    url = "https://dl.google.com/dl/android/maven2/androidx/compose/material/{}/{}/{}-{}-sources.jar".format(name,VERSION,name,VERSION)
    output=RUNNER_TMP/(name+"-"+VERSION+"-sources.jar")
    with urllib.request.urlopen(urllib.request.Request(url,headers={"User-Agent":"Arihna-CI/1.0"}),timeout=90) as conn, open(output,"wb") as f:
        shutil.copyfileobj(conn,f)
    with zipfile.ZipFile(output) as z:
        valid=z.testzip()
        assert valid is None, (name,valid)
    print("OFFICIAL-SOURCE",url,"sha256",sha256(output),flush=True)
    return output,url

def main():
    assert shell("git","rev-parse","HEAD")==shell("git","rev-parse","driver/apk-size-icons-preflight-20261017")
    shell("git","fetch","origin","main","spec/apk-size-icons-20261016")
    assert shell("git","rev-parse","origin/main")==BASE
    assert shell("git","rev-parse","origin/spec/apk-size-icons-20261016")==SPEC
    assert shell("git","rev-parse",SPEC+"^")==BASE
    assert not shell("git","ls-remote","--heads","origin",BRANCH), "Candidate already exists; do not rewrite it"

    shell("git","checkout","-B",BRANCH,SPEC)
    kotlin = list(SRC.rglob("*.kt"))
    usepattern=re.compile(r"\\bIcons\\.Rounded\\.([A-Za-z_][A-Za-z_0-9]*)")
    imports_pattern=re.compile(r"^import androidx\\.compose\\.material\\.icons\\.rounded\\.([A-Za-z_][A-Za-z_0-9]*)$",re.M)
    otherpattern=re.compile(r"\\bIcons\\.(Filled|Outlined|Sharp|TwoTone|Default)\\.")
    used=set()
    byfile={}
    for path in kotlin:
        contents=path.read_text()
        assert not otherpattern.search(contents), "Non-Rounded icon requires explicit review: "+str(path)
        refs=set(usepattern.findall(contents))
        imports=set(imports_pattern.findall(contents))
        assert refs<=imports, (path,refs-imports)
        assert "import androidx.compose.material.icons.rounded.*" not in contents, path
        used|=refs
        if imports:
            byfile[str(path.relative_to(REPO))]=(refs,imports)
            # Drop unused imports, not visible/UI behavior.
            for icon in sorted(imports-refs):
                contents=contents.replace("import androidx.compose.material.icons.rounded."+icon+"\\n","")
            path.write_text(contents)
    assert len(used)>=15, used
    print("ICON-INVENTORY count",len(used),"total-Kotlin-files",len(kotlin))
    for k,(refs,imports) in sorted(byfile.items()):
        print("FILE",k,"used",",".join(sorted(refs)),"unused",",".join(sorted(imports-refs)))
    print("USED",",".join(sorted(used)),flush=True)

    sources={}
    for name in ("material-icons-core","material-icons-extended"):
        jar,url=fetch_artifact(name)
        sources[name]={"jar":jar,"url":url,"sha":sha256(jar)}
    dest = SRC / "androidx/compose/material/icons/rounded"
    dest.mkdir(parents=True,exist_ok=True)
    source_kinds={}
    for icon in sorted(used):
        member_suffix="/rounded/"+icon+".kt"
        options=[]
        for name,meta in sources.items():
            with zipfile.ZipFile(meta["jar"]) as z:
                entries=[p for p in z.namelist() if p.endswith(member_suffix)]
                options.extend((name,p) for p in entries)
        assert len(options)==1, (icon,options)
        kind,member=options[0]
        source_kinds[icon]=kind
        if kind=="material-icons-extended":
            with zipfile.ZipFile(sources[kind]["jar"]) as z:
                data=z.read(member)
            assert b"package androidx.compose.material.icons.rounded" in data, icon
            (dest/(icon+".kt")).write_bytes(data)
        print("SOURCE",icon,kind,member,flush=True)
    assert any(v=="material-icons-extended" for v in source_kinds.values())

    gradle=REPO/"app/build.gradle.kts"
    before=gradle.read_text()
    assert before.count("implementation(libs.androidx.compose.material.icons.extended)")==1
    gradle.write_text(before.replace("implementation(libs.androidx.compose.material.icons.extended)","implementation(libs.androidx.compose.material.icons.core)"))
    toml=REPO/"gradle/libs.versions.toml"
    before=toml.read_text()
    old='androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }'
    new='androidx-compose-material-icons-core = { module = "androidx.compose.material:material-icons-core" }'
    assert before.count(old)==1
    toml.write_text(before.replace(old,new))

    prov=REPO/"docs/size/MATERIAL_ICONS_SOURCE.md"
    prov.parent.mkdir(parents=True,exist_ok=True)
    lines=[
        "# Arihna Material Rounded icons - pinned AndroidX source",
        "",
        "Source: official Google Maven AndroidX 1.7.8 sources jars; resolved by Compose BOM 2026.08.00.",
        "Apache License 2.0, original AndroidX generated source copied unmodified.",
        "Only required extended icon sources are vendored. Core icon definitions remain in material-icons-core.",
        "",
        "| Maven artifact | sources.jar SHA-256 | Official artifact URL |",
        "| --- | --- | --- |",
    ]
    for name,meta in sources.items():
        lines.append("| "+name+":1.7.8 | "+meta["sha"]+" | "+meta["url"]+" |")
    lines += ["","| Rounded icon | Origin |","| --- | --- |"]
    for icon in sorted(used):
        lines.append("| "+icon+" | "+source_kinds[icon]+" |")
    lines += ["","Source inventory generated by exact source reference scan across app/src/main/java; unused icon imports dropped.",""]
    prov.write_text("\\n".join(lines))

    geofile=REPO/"app/src/main/assets/geonames/cities.db"
    geofile.parent.mkdir(parents=True,exist_ok=True)
    assert geofile.is_file() and sha256(geofile)==GEO_SHA, "GeoNames restore missing/mismatch"

    # Pre-candidate compilation: prohibit candidate creation on compile/unit failure.
    subprocess.run(["./gradlew","--no-daemon",":app:testDebugUnitTest",":app:compileDebugAndroidTestKotlin",":app:assembleDebug","--stacktrace"],check=True)
    apk=REPO/"app/build/outputs/apk/debug/app-debug.apk"
    assert apk.is_file()
    stable=Path("/tmp/arihna-icons-preflight/arihna-stable.apk")
    assert stable.is_file() and stable.stat().st_size==STABLE_SIZE and sha256(stable)==STABLE_HASH

    with zipfile.ZipFile(stable) as old, zipfile.ZipFile(apk) as newz:
        def dex(z):
            files=[i for i in z.infolist() if re.fullmatch(r"classes(?:\\d+)?\\.dex",i.filename)]
            return sum(i.compress_size for i in files),sum(i.file_size for i in files),len(files)
        old_dex,new_dex=dex(old),dex(newz)
        def protected(z):
            return {n for n in z.namelist() if (
                n.startswith("assets/") and any(s in n.lower() for s in ("mushaf","quran","geonames","warsh","tanzil"))
            ) or n.startswith("res/raw/")}
        old_assets,new_assets=protected(old),protected(newz)
        assert old_assets==new_assets, ("Offline asset names changed",sorted(old_assets-new_assets)[:10],sorted(new_assets-old_assets)[:10])
        assert len(old_assets)>1200, "Unexpectedly few Quran/geo/audio files"
        for n in sorted(old_assets):
            if hashlib.sha256(old.read(n)).digest()!=hashlib.sha256(newz.read(n)).digest():
                raise RuntimeError("Protected offline asset contents changed: "+n)
    delta=STABLE_SIZE-apk.stat().st_size
    dex_delta=old_dex[0]-new_dex[0]
    print("MEASUREMENTS stable APK",STABLE_SIZE,"candidate APK",apk.stat().st_size,"saved",delta)
    print("DEX STABLE",old_dex,"NEW",new_dex,"saved compressed",dex_delta)
    print("PROTECTED ASSETS",len(old_assets),"ALL SHA-256 IDENTICAL")
    assert delta>1000000 and dex_delta>1000000, "Insufficient APK/DEX reduction: abort candidate"

    with prov.open("a") as f:
        f.write("\\n## Preflight build/audit\\n\\n")
        f.write("- Stable APK: "+str(STABLE_SIZE)+" bytes SHA-256 "+STABLE_HASH+"\\n")
        f.write("- Candidate debug APK: "+str(apk.stat().st_size)+" bytes SHA-256 "+sha256(apk)+"\\n")
        f.write("- APK delta: -"+str(delta)+" bytes\\n")
        f.write("- DEX compressed: "+str(old_dex[0])+" -> "+str(new_dex[0])+" bytes; delta -"+str(dex_delta)+"\\n")
        f.write("- DEX raw: "+str(old_dex[1])+" -> "+str(new_dex[1])+" bytes\\n")
        f.write("- Offline assets checked: "+str(len(old_assets))+"; exact SHA-256 parity PASS\\n")

    shell("git","config","user.name","github-actions[bot]")
    shell("git","config","user.email","41898282+github-actions[bot]@users.noreply.github.com")
    shell("git","add","app/build.gradle.kts","gradle/libs.versions.toml","app/src/main/java","docs/size/MATERIAL_ICONS_SOURCE.md")
    assert shell("git","diff","--cached","--name-only")
    shell("git","commit","-m","perf(size): vendor exact used Material Rounded icons")
    candidate=shell("git","rev-parse","HEAD")
    assert shell("git","rev-parse","HEAD^")==SPEC
    assert shell("git","rev-list","--count",SPEC+".."+candidate)=="1"
    shell("git","push","origin","HEAD:refs/heads/"+BRANCH)
    print("CANDIDATE_CREATED",candidate,"parent",SPEC,flush=True)
    if os.getenv("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"],"a") as f:
            f.write("## Preflight PASS\\nCandidate: "+candidate+"\\nAPK reduction: "+str(delta)+" bytes\\nDEX reduction: "+str(dex_delta)+" bytes\\n")

if __name__=="__main__":
    main()
