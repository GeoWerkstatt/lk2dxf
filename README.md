[![CI](https://github.com/GeoWerkstatt/lk2dxf/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/GeoWerkstatt/lk2dxf/actions/workflows/ci.yml)
[![Release](https://github.com/GeoWerkstatt/lk2dxf/actions/workflows/release.yml/badge.svg)](https://github.com/GeoWerkstatt/lk2dxf/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/GeoWerkstatt/lk2dxf)](https://github.com/GeoWerkstatt/lk2dxf/releases/latest)
[![License](https://img.shields.io/github/license/GeoWerkstatt/lk2dxf)](https://github.com/GeoWerkstatt/lk2dxf/blob/main/LICENSE)

# lk2dxf

The `lk2dxf` tool can be used to create a DXF file conforming to `SIA 405 SN 532405` from one or more INTERLIS XTF files of the LKMap model `SIA405_LKMap_2015_LV95`.

## Requirements

Java 21 (LTS) or later is required to run `lk2dxf`.
Required Jar dependencies are bundled with the distribution of the tool.

A [docker image](https://github.com/GeoWerkstatt/lk2dxf/pkgs/container/lk2dxf) containing all necessary dependencies is also available for download.

## Usage

Starting from JAR:
```shell
java -jar lk2dxf.jar [options] <XTF input files ...> <DXF output file>
```

Starting with Docker:
```shell
docker run -it --rm -v ${PWD}:/host ghcr.io/geowerkstatt/lk2dxf [options] <XTF inputs inside volume: /host/**/*.xtf> <DXF output inside volume: /host/**/*.dxf>
```

### Commandline Options
| Option | Description |
| --- | --- |
| --help | Show help message and exit |
| --version | Show version information and exit |
| --perimeter \<wkt\> | The WKT of a polygon used to filter the objects |
| --logfile \<file\> | Path to the logfile |
| --trace | Enable trace logging |

### Perimeter

The `--perimeter` option can be used to filter the objects written the output DXF file.
Well-known text (WKT) syntax is used to specify the polygon of the perimeter.
Only geometries that intersect the perimeter are included in the DXF file and all objects whose geometry is fully outside the specified perimeter are excluded.

Existing geometries are not modified, which means that some geometries of the DXF file may extend beyond the bounds of the perimeter.
