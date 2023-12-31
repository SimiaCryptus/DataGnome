package com.simiacryptus.util.index

@JvmInline value class TokenCount(val tokenIndex: Long) : Comparable<TokenCount> {
  val asInt get() = tokenIndex.toInt()
  override operator fun compareTo(other: TokenCount) = tokenIndex.compareTo(other.tokenIndex)
  operator fun plus(other: TokenCount) = TokenCount(tokenIndex + other.tokenIndex)
  operator fun minus(other: TokenCount) = TokenCount(tokenIndex - other.tokenIndex)
  operator fun plus(other: Long) = TokenCount(tokenIndex + other)
  operator fun minus(other: Long) = TokenCount(tokenIndex - other)
  operator fun plus(other: Int) = TokenCount(tokenIndex + other)
  operator fun minus(other: Int) = TokenCount(tokenIndex - other)
  operator fun rem(other: TokenCount) = TokenCount(tokenIndex % other.tokenIndex)
  operator fun rem(other: Long) = TokenCount(tokenIndex % other)
}

infix fun TokenCount.until(to: TokenCount): Iterable<TokenCount> = this.tokenIndex.until(to.tokenIndex).map { TokenCount(it) }