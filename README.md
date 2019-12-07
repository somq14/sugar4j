# sugar4j 

## 概要
sugar4jを使えば, Javaプログラムから簡単にSATソルバを利用することができます.
整数変数を含む制約をSATソルバへの入力に変換し, CSP (制約充足問題) を解くことができます.
CSPの求解を繰り返すことで, COP (制約最適化問題) を解くことができます.

CSPからCNFへのエンコードには, SAT型制約ソルバ [Sugar](http://bach.istc.kobe-u.ac.jp/sugar/) を使っています.

## 環境構築 (on Ubuntu)
### ツールの準備
- git
- Java8
- Maven (mvn)
```
$ sudo apt install git openjdk-8-headless mvn
```

### ソースのダウンロード
```
$ git clone git@github.com:somq14/sugar4j.git
```

### (任意) 指定バージョンのチェックアウト
```
$ git checkout -b v1.0.0
```
指定可能なバージョンは [こちらを参照.](https://github.com/somq14/sugar4j/releases)

### sugar4jのビルド
```
$ mvn package -Dmaven.skip.Test=true
$ ls ./target
```

### (任意) ipasir4jのコンパイル
この手順を行うと `Ipasir4jSolver` クラスが使用できます.
`Ipasir4jSolver` クラスは `IpasirSolver` クラスに比べて, 効率的に SATソルバに制約を与えます.
大きな問題を解く場合は, `Ipasir4jSolver` クラスの使用を勧めます.

```
$ cd ipasir4j
$ make
$ sudo make install
```

共有ライブラリ `libipasir4j.so` が配置されていればOK.
```
$ ls -l /usr/local/lib/libipasir4j.so
```

### SATソルバのインストール
TODO

## 使用例
### SATを解く
TODO

### CSPを解く
TODO

### COPを解く
TODO

## ドキュメント
- Javadoc - https://somq14.github.io/sugar4j
- Sugar の CSPモデル - http://bach.istc.kobe-u.ac.jp/sugar/current/docs/syntax.html
