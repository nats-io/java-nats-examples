![NATS](../images/large-logo.png)

# File Transfer

The file transfer example demonstrates using JetStream as a way to store a large block of data such as a file.
It operates by breaking that large amount of data into smaller parts, each part being one message.
Depending on the block size you set, there could be thousands of messages that make up one large piece of data.

### JetStream Setup

The setup requires that we create a key-value bucket where there is one key per file and
a stream with a wildcard subject (i.e. `parts.*`) for the parts of files. 
Each file's parts are stored under one specific subject, the wildcard replaced with a file id as in `parts.<id>`
The id is made from a digest of information about the file. See the `FileMeta.java` constructor for details.

There is code in the `Runner.java` to do this.

### Constants

The `Constants.java` has data used to run the examples. Most other classes reference the data here.

### Uploader

The `Uploader.java` class takes one file from disk and puts it in the stream. It places a key in the key-value store,
the value of the key being the json serialization of the `FileMeta.java` object

```java
public static void upload(
        Connection conn, 
        int partSize, 
        File f, 
        String description, 
        String contentType, 
        boolean gzip
) throws IOException, JetStreamApiException, NoSuchAlgorithmException {
```

- conn: The connection to JetStream
- partSize: The size of each part. The example code is set to 8192 bytes.
- f: The input File object.
- description: A description of the file.
- contentType: The mime type of the file. See `Constants.java` for examples.
- gzip: boolean whether to compress the contents. Works great for text, not so good for binary.

### Downloader

The `Downloader.java` class takes a `FileMeta` object retrieved from the Key-Value bucket and uses that to
read all the messages from a stream and write them to a file on the disk.

> Note: The Downloader code is written with manual handling of "Flow Control" and manual ensuring of part ordering.
> This is because at the time of the writing of the example, automated "Flow Control" and "Ordered Consumer" are not in the client.
> This example will be changed when that code exists.

### FileMeta

The FileMeta contains information about the file. It is serialized as json and stored as the value in the key-value store under
a key that is a safe version of the file name. This may not be sufficient for a real world data store where there could be
files with the same name, or even versions of the file, but we'll leave that exercise for some other time.

This information is useful when downloading the file, for instance to double check the final length and to ensure the crc check matches.

Here is an example:

```json
{
	"id": "25D794863363268D4A15FC4D546DA2F62EEEFBE56C1CAF83CF00FA7247E27F3E",
	"name": "text-10000000.txt",
	"desc": "10000000 bytes text file",
	"contentType": "text/plain",
	"length": 10000000,
	"parts": 1221,
	"partSize": 8192,
	"lastPartSize": 5760,
	"fileDate": 1628104043121,
	"digest": "SHA-256=3F9239B0272558CE6E3E98731C43A654AC6882C5050D5F352B2A5BFBB8DE0058"
}
```

### PartMeta

Each part of the file is published as payload for a message on the stream. The meta data for the part is put as headers for the message.
This information is useful when downloading the file, to know whether the part should be decompressed and what algorithm to use
instance to double check the final length or the part and to ensure the crc check for the specific part matches. 
It also maintains its own part number to help ensure that the parts are put together in order.  

Here is an example:

```
io.nats.part.length:1808
Digest:SHA-256=696F6A1618E827FD2BBC2C6A9FF3395308B4580FE44DB4265D449C32D5899B66
io.nats.part.encoded-length:240
Content-Encoding:gzip
io.nats.part.id:385DF1E67CC696270224362783D6CFEFFC5953CCD220BBFC26E3F714355FAE07.2
io.nats.part.part-number:2
io.nats.part.start:8192
```

### Runner

The `Runner.java` class is just a main that can be used to exercise the code.

- inputs: This array is used to setup which files to use as input for the run.
- generateFiles(): helper function to generate test files. Overwrites existing files.
- setup(): helper function to setup the bucket and stream
- upload(): helper function to upload files
- download(): helper function to download files. Clears away previous downloaded files.
- listParts(): helper function to list the parts for specified files.
- listFilesAkaBucketKeys(): helper function to list the files store by looking at the bucket keys

