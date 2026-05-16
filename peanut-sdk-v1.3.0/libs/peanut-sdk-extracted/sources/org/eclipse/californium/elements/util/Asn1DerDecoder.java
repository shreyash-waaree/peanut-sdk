package org.eclipse.californium.elements.util;

import com.keenon.common.error.PeanutError;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Asn1DerDecoder.class */
public class Asn1DerDecoder {
    public static final String EC = "EC";
    public static final String RSA = "RSA";
    public static final String DSA = "DSA";
    public static final String ECv2 = "EC.v2";
    public static final String X25519 = "X25519";
    public static final String X25519v2 = "X25519.v2";
    public static final String X448 = "X448";
    public static final String X448v2 = "X448.v2";
    public static final String OID_X25519 = "OID.1.3.101.110";
    public static final String OID_XD448 = "OID.1.3.101.111";
    public static final int EC_PUBLIC_KEY_UNCOMPRESSED = 4;
    private static final int MAX_DEFAULT_LENGTH = 65536;
    private static final int TAG_SEQUENCE = 48;
    private static final int TAG_SET = 49;
    private static final int MAX_OID_LENGTH = 32;
    private static final int TAG_OID = 6;
    private static final int TAG_INTEGER = 2;
    private static final int TAG_OCTET_STRING = 4;
    private static final int TAG_BIT_STRING = 3;
    private static final int TAG_UTF8_STRING = 12;
    private static final int TAG_PRINTABLE_STRING = 19;
    private static final int TAG_TELETEX_STRING = 20;
    private static final int TAG_UNIVERSAL_STRING = 28;
    private static final int TAG_BMP_STRING = 30;
    private static final Charset UCS_2;
    private static final Charset UCS_4;
    private static final int[] TAGS_STRING = {12, 19, 30, 28, 20};
    private static final byte[] OID_RSA_PUBLIC_KEY = {42, -122, 72, -122, -9, 13, 1, 1, 1};
    private static final byte[] OID_DH_KEY_AGREEMENT = {42, -122, 72, -122, -9, 13, 1, 3, 1};
    private static final byte[] OID_DH_PUBLIC_KEY = {42, -122, 72, -50, 62, 2, 1};
    private static final byte[] OID_DSA_PUBLIC_KEY = {42, -122, 72, -50, 56, 4, 1};
    private static final byte[] OID_EC_PUBLIC_KEY = {42, -122, 72, -50, 61, 2, 1};
    private static final byte[] OID_X25519_PUBLIC_KEY = {43, 101, 110};
    private static final byte[] OID_X448_PUBLIC_KEY = {43, 101, 111};
    private static final byte[] OID_ED25519_PUBLIC_KEY = {43, 101, 112};
    private static final byte[] OID_ED448_PUBLIC_KEY = {43, 101, 113};
    private static final byte[] OID_CN = {85, 4, 3};
    private static final EntityDefinition SEQUENCE = new EntityDefinition(48, 65536, "SEQUENCE");
    private static final EntityDefinition SET = new EntityDefinition(49, 65536, "SET");
    private static final OidEntityDefinition OID = new OidEntityDefinition();
    private static final IntegerEntityDefinition INTEGER = new IntegerEntityDefinition();
    private static final EntityDefinition BIT_STRING = new EntityDefinition(3, 65536, "BIT STRING");
    private static final EntityDefinition OCTET_STRING = new EntityDefinition(4, 65536, "OCTET STRING");
    private static final int TAG_CONTEXT_0_SPECIFIC = 160;
    private static final EntityDefinition CONTEXT_SPECIFIC_0 = new EntityDefinition(TAG_CONTEXT_0_SPECIFIC, 65536, "CONTEXT SPECIFIC 0");
    private static final int TAG_CONTEXT_1_SPECIFIC = 161;
    private static final EntityDefinition CONTEXT_SPECIFIC_1 = new EntityDefinition(TAG_CONTEXT_1_SPECIFIC, 65536, "CONTEXT SPECIFIC 1");
    private static final int TAG_CONTEXT_1_SPECIFIC_PRIMITIVE = 129;
    private static final EntityDefinition CONTEXT_SPECIFIC_PRIMITIVE_1 = new EntityDefinition(TAG_CONTEXT_1_SPECIFIC_PRIMITIVE, 65536, "CONTEXT SPECIFIC PRIMITIVE 1");
    public static final String ED25519 = "Ed25519";
    public static final String OID_ED25519 = "OID.1.3.101.112";
    public static final String EDDSA = "EdDSA";
    public static final String ED25519v2 = "Ed25519.v2";
    private static final String[] ED25519_ALIASES = {ED25519, "1.3.101.112", OID_ED25519, EDDSA, ED25519v2};
    public static final String ED448 = "Ed448";
    public static final String OID_ED448 = "OID.1.3.101.113";
    public static final String ED448v2 = "Ed448.v2";
    private static final String[] ED448_ALIASES = {ED448, "1.3.101.113", OID_ED448, EDDSA, ED448v2};
    public static final String DH = "DH";
    private static final String[][] ALGORITHM_ALIASES = {new String[]{DH, "DiffieHellman"}, ED25519_ALIASES, ED448_ALIASES};

    /* JADX WARN: Type inference failed for: r0v36, types: [java.lang.String[], java.lang.String[][]] */
    static {
        Charset charset = null;
        try {
            charset = Charset.forName("ISO-10646-UCS-2");
        } catch (Throwable th) {
        }
        UCS_2 = charset;
        Charset charset2 = null;
        try {
            charset2 = Charset.forName("ISO-10646-UCS-4");
        } catch (Throwable th2) {
        }
        UCS_4 = charset2;
        JceProviderUtil.init();
    }

    private static boolean contains(String[] set, String value) {
        for (String item : set) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String getPublicKeyAlgorithm(byte[] oid, int version) {
        String algorithm = null;
        if (Arrays.equals(oid, OID_EC_PUBLIC_KEY)) {
            algorithm = version == 0 ? EC : ECv2;
        } else if (Arrays.equals(oid, OID_RSA_PUBLIC_KEY)) {
            algorithm = version == 0 ? RSA : null;
        } else if (Arrays.equals(oid, OID_DSA_PUBLIC_KEY)) {
            algorithm = version == 0 ? DSA : null;
        } else if (Arrays.equals(oid, OID_DH_PUBLIC_KEY)) {
            algorithm = version == 0 ? DH : null;
        } else if (Arrays.equals(oid, OID_DH_KEY_AGREEMENT)) {
            algorithm = version == 0 ? DH : null;
        } else if (Arrays.equals(oid, OID_ED25519_PUBLIC_KEY)) {
            algorithm = version == 0 ? ED25519 : ED25519v2;
        } else if (Arrays.equals(oid, OID_ED448_PUBLIC_KEY)) {
            algorithm = version == 0 ? ED448 : ED448v2;
        } else if (Arrays.equals(oid, OID_X25519_PUBLIC_KEY)) {
            algorithm = version == 0 ? X25519 : X25519v2;
        } else if (Arrays.equals(oid, OID_X448_PUBLIC_KEY)) {
            algorithm = version == 0 ? X448 : X448v2;
        }
        return algorithm;
    }

    public static boolean isEcBased(String algorithm) {
        return EC.equalsIgnoreCase(algorithm) || getEdDsaStandardAlgorithmName(algorithm, null) != null;
    }

    public static byte[] readSequenceEntity(DatagramReader reader) {
        return SEQUENCE.readEntity(reader);
    }

    public static byte[] readSequenceValue(DatagramReader reader) {
        return SEQUENCE.readValue(reader);
    }

    public static byte[] readOidValue(DatagramReader reader) {
        return OID.readValue(reader);
    }

    public static String readOidString(DatagramReader reader) {
        byte[] oid = OID.readValue(reader);
        return OID.toString(oid);
    }

    public static String readSubjectPublicKeyAlgorithm(byte[] data) {
        DatagramReader reader = new DatagramReader(data, false);
        byte[] value = readOidValue(SEQUENCE.createRangeReader(SEQUENCE.createRangeReader(reader, false), false));
        return getPublicKeyAlgorithm(value, 0);
    }

    public static PublicKey readSubjectPublicKey(byte[] data) throws GeneralSecurityException {
        PublicKey publicKey = null;
        String algorithm = readSubjectPublicKeyAlgorithm(data);
        if (algorithm != null) {
            KeyFactory factory = getKeyFactory(algorithm);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(data);
            publicKey = factory.generatePublic(publicKeySpec);
        }
        return publicKey;
    }

    public static String readPrivateKeyAlgorithm(byte[] data) {
        String algorithm = null;
        DatagramReader reader = SEQUENCE.createRangeReader(new DatagramReader(data, false), false);
        byte[] readValue = INTEGER.readValue(reader);
        int version = INTEGER.toInteger(readValue);
        if (version < 0 || version > 1) {
            throw new IllegalArgumentException("Version 0x" + Integer.toHexString(version) + " not supported!");
        }
        try {
            DatagramReader sequenceReader = SEQUENCE.createRangeReader(reader, false);
            byte[] value = readOidValue(sequenceReader);
            algorithm = getPublicKeyAlgorithm(value, version);
        } catch (IllegalArgumentException ex) {
            if (version == 1) {
                OCTET_STRING.createRangeReader(reader, false);
                byte[] oid = readOidValue(CONTEXT_SPECIFIC_0.createRangeReader(reader, false));
                String oidAsString = "0x" + StringUtil.byteArray2Hex(oid);
                try {
                    oidAsString = OID.toString(oid);
                    try {
                        ECParameterSpec ecParameterSpec = getECParameterSpec(oidAsString);
                        if (ecParameterSpec != null) {
                            algorithm = ECv2;
                        }
                    } catch (GeneralSecurityException e) {
                    }
                } catch (IllegalArgumentException e2) {
                }
                if (algorithm == null) {
                    throw new IllegalArgumentException("OID " + oidAsString + " not supported!");
                }
            } else {
                throw ex;
            }
        }
        return algorithm;
    }

    public static Keys readPrivateKey(byte[] data) throws GeneralSecurityException {
        Keys keys = null;
        String algorithm = readPrivateKeyAlgorithm(data);
        if (algorithm != null) {
            if (algorithm == ED25519v2 || algorithm == ED448v2) {
                keys = readEdDsaPrivateKeyV2(data);
            } else if (algorithm == ECv2) {
                keys = readEcPrivateKeyV2(data);
            } else {
                KeyFactory factory = getKeyFactory(algorithm);
                EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(data);
                keys = new Keys();
                keys.privateKey = factory.generatePrivate(privateKeySpec);
            }
        }
        return keys;
    }

    public static Keys readEcPrivateKeyV2(byte[] data) throws GeneralSecurityException {
        Keys keys = null;
        DatagramReader reader = SEQUENCE.createRangeReader(new DatagramReader(data, false), false);
        byte[] readValue = INTEGER.readValue(reader);
        if (readValue.length == 1 && readValue[0] == 1) {
            try {
                SEQUENCE.createRangeReader(reader, false);
            } catch (IllegalArgumentException e) {
            }
            byte[] privateKeyValue = OCTET_STRING.readValue(reader);
            byte[] oid = readOidValue(CONTEXT_SPECIFIC_0.createRangeReader(reader, false));
            try {
                ECParameterSpec ecParameterSpec = getECParameterSpec(OID.toString(oid));
                int keySize = ((ecParameterSpec.getCurve().getField().getFieldSize() + 8) - 1) / 8;
                if (privateKeyValue.length != keySize) {
                    throw new GeneralSecurityException("private key size " + privateKeyValue.length + " doesn't match " + keySize);
                }
                KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateKeyValue), ecParameterSpec);
                keys = new Keys();
                keys.privateKey = KeyFactory.getInstance(EC).generatePrivate(privateKeySpec);
                DatagramReader value = BIT_STRING.createRangeReader(CONTEXT_SPECIFIC_1.createRangeReader(reader, false), false);
                int unusedBits = value.read(8);
                if (unusedBits == 0) {
                    keys.publicKey = readEcPublicKey(value, ecParameterSpec);
                }
            } catch (IllegalArgumentException e2) {
                throw new GeneralSecurityException(e2.getMessage(), e2);
            } catch (GeneralSecurityException e3) {
                throw e3;
            }
        }
        return keys;
    }

    public static ECPublicKey readEcPublicKey(DatagramReader reader, ECParameterSpec ecParameterSpec) throws GeneralSecurityException {
        int left;
        int keySize = ((ecParameterSpec.getCurve().getField().getFieldSize() + 8) - 1) / 8;
        int compress = reader.read(8);
        int left2 = reader.bitsLeft() / 8;
        if (compress == 4 && left2 % 2 == 0 && (left = left2 / 2) == keySize) {
            BigInteger x = new BigInteger(1, reader.readBytes(left));
            BigInteger y = new BigInteger(1, reader.readBytes(left));
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(x, y), ecParameterSpec);
            return (ECPublicKey) KeyFactory.getInstance(EC).generatePublic(publicKeySpec);
        }
        return null;
    }

    public static Keys readEdDsaPrivateKeyV2(byte[] data) throws GeneralSecurityException {
        Keys keys = null;
        DatagramReader reader = SEQUENCE.createRangeReader(new DatagramReader(data, false), false);
        byte[] readValue = INTEGER.readValue(reader);
        if (readValue.length == 1 && readValue[0] == 1) {
            byte[] keyAlgorithm = SEQUENCE.readEntity(reader);
            DatagramReader oidReader = new DatagramReader(keyAlgorithm, false);
            byte[] oidValue = readOidValue(SEQUENCE.createRangeReader(oidReader, false));
            String algorithm = OID.toString(oidValue);
            byte[] privateKeyValue = OCTET_STRING.readEntity(reader);
            CONTEXT_SPECIFIC_0.createRangeReader(reader, false);
            KeyFactory factory = getKeyFactory(algorithm);
            keys = new Keys();
            DatagramWriter privateKey = new DatagramWriter(48);
            privateKey.writeByte((byte) 48);
            int positionLen = privateKey.space(8);
            privateKey.writeByte((byte) 2);
            privateKey.writeByte((byte) 1);
            privateKey.writeByte((byte) 0);
            privateKey.writeBytes(keyAlgorithm);
            privateKey.writeBytes(privateKeyValue);
            privateKey.writeSize(positionLen, 8);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey.toByteArray());
            keys.privateKey = factory.generatePrivate(privateKeySpec);
            DatagramWriter publicKey = new DatagramWriter(44);
            publicKey.writeByte((byte) 48);
            int positionLen2 = publicKey.space(8);
            publicKey.writeBytes(keyAlgorithm);
            publicKey.writeByte((byte) 3);
            int positionBits = publicKey.space(8);
            publicKey.writeBytes(CONTEXT_SPECIFIC_PRIMITIVE_1.readValue(reader));
            publicKey.writeSize(positionBits, 8);
            publicKey.writeSize(positionLen2, 8);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey.toByteArray());
            keys.publicKey = factory.generatePublic(publicKeySpec);
        }
        return keys;
    }

    public static boolean equalKeyAlgorithmSynonyms(String keyAlgorithm1, String keyAlgorithm2) {
        if (keyAlgorithm1 != null && keyAlgorithm1.equals(keyAlgorithm2)) {
            return true;
        }
        for (String[] aliases : ALGORITHM_ALIASES) {
            if (contains(aliases, keyAlgorithm1) && contains(aliases, keyAlgorithm2)) {
                return true;
            }
        }
        return false;
    }

    public static ECParameterSpec getECParameterSpec(String oid) throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(EC);
        keyPairGenerator.initialize(new ECGenParameterSpec(oid));
        ECPublicKey apub = (ECPublicKey) keyPairGenerator.generateKeyPair().getPublic();
        return apub.getParams();
    }

    public static String getEdDsaStandardAlgorithmName(String algorithm, String def) {
        if (EDDSA.equalsIgnoreCase(algorithm)) {
            return EDDSA;
        }
        if (contains(ED25519_ALIASES, algorithm)) {
            return OID_ED25519;
        }
        if (contains(ED448_ALIASES, algorithm)) {
            return OID_ED448;
        }
        return def;
    }

    public static KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        String standardAlgorithm = getEdDsaStandardAlgorithmName(algorithm, algorithm);
        return KeyFactory.getInstance(standardAlgorithm);
    }

    public static KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        String standardAlgorithm = getEdDsaStandardAlgorithmName(algorithm, algorithm);
        return KeyPairGenerator.getInstance(standardAlgorithm);
    }

    public static String readCNFromDN(byte[] data) {
        DatagramReader reader = SEQUENCE.createRangeReader(new DatagramReader(data, false), false);
        while (reader.bytesAvailable()) {
            DatagramReader setReader = SET.createRangeReader(reader, false);
            while (setReader.bytesAvailable()) {
                DatagramReader subReader = SEQUENCE.createRangeReader(setReader, false);
                byte[] oid = OID.readValue(subReader);
                if (Arrays.equals(oid, OID_CN)) {
                    try {
                        StringEntityDefinition value = new StringEntityDefinition(TAGS_STRING);
                        return value.readStringValue(subReader);
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        }
        return null;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Asn1DerDecoder$Keys.class */
    public static class Keys {
        private PrivateKey privateKey;
        private PublicKey publicKey;

        public Keys() {
        }

        public Keys(PrivateKey privateKey, PublicKey publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        public void add(Keys keys) {
            if (keys.privateKey != null) {
                this.privateKey = keys.privateKey;
            }
            if (keys.publicKey != null) {
                this.publicKey = keys.publicKey;
            }
        }

        public PrivateKey getPrivateKey() {
            return this.privateKey;
        }

        public void setPrivateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
        }

        public PublicKey getPublicKey() {
            return this.publicKey;
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Asn1DerDecoder$EntityDefinition.class */
    private static class EntityDefinition {
        private static final int HEADER_LENGTH = 2;
        private final int expectedTag;
        private final int maxLength;
        private final String description;

        public EntityDefinition(int expectedTag, int maxLength, String description) {
            this.expectedTag = expectedTag;
            this.maxLength = maxLength;
            this.description = description;
        }

        public boolean checkTag(int tag) {
            return tag == this.expectedTag;
        }

        public byte[] readEntity(DatagramReader reader) {
            return read(reader, true);
        }

        public byte[] readValue(DatagramReader reader) {
            return read(reader, false);
        }

        public byte[] read(DatagramReader reader, boolean entity) {
            int length = readLength(reader, entity);
            return reader.readBytes(length);
        }

        public DatagramReader createRangeReader(DatagramReader reader, boolean entity) {
            int length = readLength(reader, entity);
            return reader.createRangeReader(length);
        }

        public int readLength(DatagramReader reader, boolean entity) {
            int leftBytes = reader.bitsLeft() / 8;
            if (leftBytes < 2) {
                throw new IllegalArgumentException(String.format("Not enough bytes for %s! Required %d, available %d.", this.description, 2, Integer.valueOf(leftBytes)));
            }
            reader.mark();
            int tag = reader.read(8);
            if (!checkTag(tag)) {
                reader.reset();
                throw new IllegalArgumentException(String.format("No %s, found %02x instead of %02x!", this.description, Integer.valueOf(tag), Integer.valueOf(this.expectedTag)));
            }
            int length = reader.read(8);
            int entityLength = length + 2;
            if (length > 127) {
                int length2 = length & PeanutError.UnsupportedOperationException;
                if (length2 > 4) {
                    throw new IllegalArgumentException(String.format("%s length-size %d too long!", this.description, Integer.valueOf(length2)));
                }
                int leftBytes2 = reader.bitsLeft() / 8;
                if (length2 > leftBytes2) {
                    throw new IllegalArgumentException(String.format("%s length %d exceeds available bytes %d!", this.description, Integer.valueOf(length2), Integer.valueOf(leftBytes2)));
                }
                byte[] lengthBytes = reader.readBytes(length2);
                length = 0;
                for (byte b : lengthBytes) {
                    length = (length << 8) + (b & 255);
                }
                entityLength = length + 2 + lengthBytes.length;
            }
            if (length > this.maxLength) {
                throw new IllegalArgumentException(String.format("%s lenght %d too large! (supported maxium %d)", this.description, Integer.valueOf(length), Integer.valueOf(this.maxLength)));
            }
            int leftBytes3 = reader.bitsLeft() / 8;
            if (length > leftBytes3) {
                throw new IllegalArgumentException(String.format("%s lengh %d exceeds available bytes %d!", this.description, Integer.valueOf(length), Integer.valueOf(leftBytes3)));
            }
            if (entity) {
                reader.reset();
                length = entityLength;
            }
            return length;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Asn1DerDecoder$OidEntityDefinition.class */
    private static class OidEntityDefinition extends EntityDefinition {
        public OidEntityDefinition() {
            super(6, 32, "OID");
        }

        public String toString(byte[] oid) {
            StringBuilder result = new StringBuilder();
            int value = oid[0] & 255;
            result.append(value / 40).append(".").append(value % 40);
            int index = 1;
            while (index < oid.length) {
                byte bValue = oid[index];
                if (bValue < 0) {
                    int value2 = bValue & 127;
                    index++;
                    if (index == oid.length) {
                        throw new IllegalArgumentException("Invalid OID 0x" + StringUtil.byteArray2Hex(oid));
                    }
                    result.append(".").append((value2 << 7) | (oid[index] & 127));
                } else {
                    result.append(".").append((int) bValue);
                }
                index++;
            }
            return result.toString();
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Asn1DerDecoder$IntegerEntityDefinition.class */
    private static class IntegerEntityDefinition extends EntityDefinition {
        public IntegerEntityDefinition() {
            super(2, 65536, "INTEGER");
        }

        public int toInteger(byte[] integerByteArray) {
            if (integerByteArray == null) {
                throw new NullPointerException("INTEGER byte array must not be null!");
            }
            if (integerByteArray.length == 0) {
                throw new IllegalArgumentException("INTEGER byte array must not be empty!");
            }
            if (integerByteArray.length > 4) {
                throw new IllegalArgumentException("INTEGER byte array " + integerByteArray.length + " bytes is too large for int (max. 4 bytes)!");
            }
            byte sign = integerByteArray[0];
            int result = sign;
            for (int index = 1; index < integerByteArray.length; index++) {
                result = (result << 8) | (integerByteArray[index] & 255);
            }
            if ((sign >= 0) ^ (result >= 0)) {
                throw new IllegalArgumentException("INTEGER byte array value overflow!");
            }
            return result;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Asn1DerDecoder$StringEntityDefinition.class */
    private static class StringEntityDefinition extends EntityDefinition {
        private int tag;
        private int[] expectedTags;

        public StringEntityDefinition(int... expectedTags) {
            super(expectedTags[0], 65536, "STRING");
            this.expectedTags = expectedTags;
        }

        @Override // org.eclipse.californium.elements.util.Asn1DerDecoder.EntityDefinition
        public boolean checkTag(int tag) {
            for (int expectedTag : this.expectedTags) {
                if (expectedTag == tag) {
                    this.tag = tag;
                    return true;
                }
            }
            return false;
        }

        public String readStringValue(DatagramReader reader) {
            byte[] stringByteArray = readValue(reader);
            if (stringByteArray != null) {
                if (this.tag == 19) {
                    return new String(stringByteArray, StandardCharsets.US_ASCII);
                }
                if (this.tag == 12) {
                    return new String(stringByteArray, StandardCharsets.UTF_8);
                }
                if (this.tag == 30) {
                    if (Asn1DerDecoder.UCS_2 == null) {
                        throw new IllegalArgumentException("BMP_STRING not supported!");
                    }
                    return new String(stringByteArray, Asn1DerDecoder.UCS_2);
                }
                if (this.tag == 28) {
                    if (Asn1DerDecoder.UCS_2 == null) {
                        throw new IllegalArgumentException("UNIVERSAL_STRING not supported!");
                    }
                    return new String(stringByteArray, Asn1DerDecoder.UCS_4);
                }
                if (this.tag == 20) {
                    throw new IllegalArgumentException("TELETEX_STRING not supported!");
                }
                return null;
            }
            return null;
        }
    }
}
