<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" encoding="UTF-8" indent="yes"/>

  <xsl:param name="userSkillLevel" select="/recipeData/@userSkillLevel"/>

  <xsl:template match="/">
    <table class="xsl-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Title</th>
          <th>Cuisine 1</th>
          <th>Cuisine 2</th>
          <th>Difficulty</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="//recipe"/>
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="recipe">
    <xsl:variable name="rowClass">
      <xsl:choose>
        <xsl:when test="difficulty = $userSkillLevel">match-row</xsl:when>
        <xsl:otherwise>other-row</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <tr class="{$rowClass}">
      <td><xsl:value-of select="position()"/></td>
      <td><xsl:value-of select="title"/></td>
      <td><xsl:value-of select="cuisine1"/></td>
      <td><xsl:value-of select="cuisine2"/></td>
      <td><xsl:value-of select="difficulty"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
