package com.achain.data;

import com.achain.util.Base58;
import com.achain.util.ECC;
import com.achain.util.MyByte;
import com.achain.util.RIPEMD160;
import com.achain.util.SHA;

import org.bouncycastle.jce.interfaces.ECPrivateKey;

import java.math.BigInteger;

public class ACTPrivateKey {

  private String keyStr;
  private byte[] encoded;
  private BigInteger d;
  private ECPrivateKey ecPrivateKey;
  private byte[] publicKey;
  private byte[] publicKeyCompressed;
  private ACTAddress actAddress;

  public ACTPrivateKey(String keyStr) {
    this.keyStr = keyStr;
    encoded = Base58.decode(keyStr);
    if (!check(encoded)) {
      throw new RuntimeException("wrong private key format!");
    }
    encoded = MyByte.copyBytes(encoded, 1, 32);
  }

  public ACTPrivateKey(byte[] encoded) {
    if (encoded.length != 32) {
      throw new RuntimeException("the length of private key must be 32 byte");
    }
    this.encoded = encoded;
  }

  public ACTPrivateKey() {
    d = ((ECPrivateKey) ECC.generate().getPrivate()).getD();
    while (d.toByteArray().length < 32){
      d = ((ECPrivateKey) ECC.generate().getPrivate()).getD();
    }
    encoded = MyByte.copyBytesR(d.toByteArray(), 32);
  }

  private boolean check(byte[] wifBytes) {
    if (wifBytes.length != 37) {
      return false;
    }
    byte[] checksum = SHA._256hash(MyByte.copyBytes(wifBytes, 33));
    return checksum(wifBytes, checksum) ||
           checksum(wifBytes, SHA._256hash(checksum));
  }

  private boolean checksum(byte[] wifBytes, byte[] checksum) {
    for (int i = 0; i < 4; i++) {
      if (wifBytes[wifBytes.length - 4 + i] != checksum[i]) {
        return false;
      }
    }
    return true;
  }

  public String getKeyStr() {
    if (keyStr == null) {
      byte[] temp = MyByte.builder().copy((byte) 0x80).copy(encoded).getData();
      keyStr = Base58.encode(
          MyByte.builder()
                .copy(temp)
                .copy(SHA._256hash(SHA._256hash(temp)), 4)
                .getData());
    }
    return keyStr;
  }

  public byte[] getEncoded() {
    return encoded;
  }

  public ECPrivateKey getECPrivateKey() {
    if (ecPrivateKey == null) {
      ecPrivateKey = ECC.loadPrivateKey(encoded);
    }
    return ecPrivateKey;
  }

  //
  public byte[] getPublicKey(boolean compressed) {
    byte[] key = compressed ? publicKeyCompressed : publicKey;
    if (key == null) {
      key = ECC.calculatePublicKey(getD(), compressed);
      if (compressed) {
        publicKeyCompressed = key;
      } else {
        publicKey = key;
      }
    }
    return key;
  }


  public ACTAddress getAddress() {
    if (actAddress == null) {
      actAddress = new ACTAddress(RIPEMD160.hash(SHA._512hash(getPublicKey(true))), ACTAddress.Type.ADDRESS);
    }
    return actAddress;
  }

  public BigInteger getD() {
    if (d == null) {
      d = new BigInteger(1, encoded);
    }
    return d;
  }
}
